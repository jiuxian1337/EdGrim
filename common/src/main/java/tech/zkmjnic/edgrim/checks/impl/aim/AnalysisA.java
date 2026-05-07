package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@CheckData(
        name = "AnalysisA",
        configName = "AnalysisA",
        decay = 0.05,
        description = "attack-centered click jitter analysis"
)
public final class AnalysisA extends Check implements RotationCheck {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int MAX_BUFFERED_UPDATES = 64;
    private static final int TOP_MATCH_COUNT = 3;
    private static final double MIN_SAMPLE_ENERGY = 1.25;
    private static final double MIN_CLASSIFICATION_SCORE = 0.86;
    private static final double MIN_CLASSIFICATION_MARGIN = 0.012;
    private static final String ROOT_DIR = "analysis-a";
    private static final String LEGIT_DIR = "legit";
    private static final String CHEAT_DIR = "cheat";
    private static final String RECORDED_DIR = "recorded";
    private static volatile ReferenceLibrary referenceLibrary;

    private ArrayDeque<RotationFrame> tickBuffer;
    private ArrayDeque<Long> attacksAwaitingCenter;
    private ArrayDeque<PendingSample> pendingSamples;
    private ArrayDeque<SampleDecision> analysisWindow;

    private boolean recordMode;
    private boolean debugLog;
    private int sampleRadius = 5;
    private int sampleSize = 11;
    private int analysisWindowSize = 10;
    private int minCheatVotes = 6;
    private double minAverageCheatSimilarity = 0.89;
    private double minAverageMargin = 0.02;
    private long rotationSequence;

    public AnalysisA(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        ensureRuntimeState();
        recordMode = config.getBooleanElse("AnalysisA.record-mode", false);
        debugLog = config.getBooleanElse("AnalysisA.debug-log", false);
        sampleRadius = Math.max(1, config.getIntElse("AnalysisA.sample-radius", 5));
        sampleSize = (sampleRadius * 2) + 1;
        analysisWindowSize = Math.max(3, config.getIntElse("AnalysisA.analysis-window-size", 10));
        minCheatVotes = Math.max(1, Math.min(
                analysisWindowSize,
                config.getIntElse("AnalysisA.min-cheat-votes", Math.max(1, (analysisWindowSize / 2) + 1))
        ));
        minAverageCheatSimilarity = clamp01(config.getDoubleElse(
                "AnalysisA.min-average-cheat-similarity",
                0.89
        ));
        minAverageMargin = Math.max(0.0, config.getDoubleElse("AnalysisA.min-average-margin", 0.02));
        tickBuffer.clear();
        attacksAwaitingCenter.clear();
        pendingSamples.clear();
        analysisWindow.clear();
        referenceLibrary = ReferenceLibrary.load(sampleSize);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        ensureRuntimeState();
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        final WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        attacksAwaitingCenter.addLast(rotationSequence + 1L);
    }

    @Override
    public void process(RotationUpdate update) {
        ensureRuntimeState();
        if (update.isCinematic2()) {
            return;
        }

        appendRotation(update);
        resolveAttackCenters();
        drainPendingSamples();
    }

    private void appendRotation(RotationUpdate update) {
        rotationSequence++;
        tickBuffer.addLast(new RotationFrame(rotationSequence, update.getDeltaXRot(), update.getDeltaYRot()));
        while (tickBuffer.size() > MAX_BUFFERED_UPDATES) {
            tickBuffer.removeFirst();
        }
    }

    private void drainPendingSamples() {
        while (!pendingSamples.isEmpty()) {
            final PendingSample pending = pendingSamples.peekFirst();
            if (rotationSequence < pending.centerSequence + sampleRadius) {
                return;
            }

            pendingSamples.removeFirst();
            final RecordedAttackSample sample = extractSample(pending.centerSequence);
            if (sample == null) {
                debug("dropped attack sample because centered window was incomplete");
                continue;
            }

            if (recordMode) {
                writeRecordedSample(sample);
                continue;
            }

            if (shouldSkipDetection()) {
                continue;
            }

            analyzeSample(sample);
        }
    }

    private RecordedAttackSample extractSample(long centerSequence) {
        if (tickBuffer.size() < sampleSize) {
            return null;
        }

        final List<RotationFrame> ticks = new ArrayList<>(tickBuffer);
        int centerIndex = -1;
        for (int i = 0; i < ticks.size(); i++) {
            if (ticks.get(i).sequence == centerSequence) {
                centerIndex = i;
                break;
            }
        }

        if (centerIndex < sampleRadius || centerIndex + sampleRadius >= ticks.size()) {
            return null;
        }

        final List<RotationFrame> window = ticks.subList(centerIndex - sampleRadius, centerIndex + sampleRadius + 1);
        final List<RecordedRotation> rotations = new ArrayList<>(window.size());
        for (RotationFrame tick : window) {
            rotations.add(new RecordedRotation((float) tick.deltaYaw, (float) tick.deltaPitch));
        }
        return new RecordedAttackSample(rotations);
    }

    private void resolveAttackCenters() {
        while (!attacksAwaitingCenter.isEmpty()) {
            final long centerSequence = attacksAwaitingCenter.peekFirst();
            if (rotationSequence < centerSequence) {
                return;
            }
            attacksAwaitingCenter.removeFirst();
            pendingSamples.addLast(new PendingSample(centerSequence));
        }
    }

    private void analyzeSample(RecordedAttackSample sample) {
        final ReferenceLibrary library = getReferenceLibrary();
        if (!library.hasTemplates()) {
            debug("sample skipped because legit/cheat template library is not ready");
            return;
        }

        final WindowSignature signature = WindowSignature.fromRotations(sample.rotations, sampleRadius);
        if (signature.totalEnergy < MIN_SAMPLE_ENERGY) {
            debug(String.format("sample classified as %s (energy=%.3f below minimum)", Label.UNCERTAIN.logName(), signature.totalEnergy));
            addDecision(new SampleDecision(Label.UNCERTAIN, 0.0, 0.0));
            return;
        }

        final ReferenceScore legitScore = library.score(signature, true);
        final ReferenceScore cheatScore = library.score(signature, false);
        final SampleDecision decision = classify(legitScore, cheatScore);
        debug(String.format(
                "sample classified as %s (legit=%.3f cheat=%.3f bestLegit=%s bestCheat=%s)",
                decision.label.logName(),
                decision.legitScore,
                decision.cheatScore,
                legitScore.bestReferenceName.isBlank() ? "-" : legitScore.bestReferenceName,
                cheatScore.bestReferenceName.isBlank() ? "-" : cheatScore.bestReferenceName
        ));
        addDecision(decision);
    }

    private void addDecision(SampleDecision decision) {
        analysisWindow.addLast(decision);
        while (analysisWindow.size() > analysisWindowSize) {
            analysisWindow.removeFirst();
        }

        if (analysisWindow.size() < analysisWindowSize) {
            return;
        }

        evaluateAnalysisWindow();
    }

    private void evaluateAnalysisWindow() {
        int cheatVotes = 0;
        int legitVotes = 0;
        int uncertainVotes = 0;
        double totalCheatScore = 0.0;
        double totalLegitScore = 0.0;

        for (SampleDecision decision : analysisWindow) {
            if (decision.label == Label.CHEAT_LIKE) {
                cheatVotes++;
            } else if (decision.label == Label.LEGIT_LIKE) {
                legitVotes++;
            } else {
                uncertainVotes++;
            }
            totalCheatScore += decision.cheatScore;
            totalLegitScore += decision.legitScore;
        }

        final double averageCheatScore = totalCheatScore / analysisWindow.size();
        final double averageLegitScore = totalLegitScore / analysisWindow.size();
        final double averageMargin = averageCheatScore - averageLegitScore;
        debug(String.format(
                "window votes: cheat=%d legit=%d uncertain=%d avgCheat=%.3f avgLegit=%.3f margin=%.3f",
                cheatVotes,
                legitVotes,
                uncertainVotes,
                averageCheatScore,
                averageLegitScore,
                averageMargin
        ));
        if (cheatVotes >= minCheatVotes
                && averageCheatScore >= minAverageCheatSimilarity
                && averageMargin >= minAverageMargin) {
            if (flagAndAlert(String.format(
                    "votes=%d/%d avgCheat=%.3f avgLegit=%.3f margin=%.3f",
                    cheatVotes,
                    analysisWindow.size(),
                    averageCheatScore,
                    averageLegitScore,
                    averageMargin
            ))) {
                player.mitigateDamage();
            }
        } else {
            reward();
        }
    }

    private SampleDecision classify(ReferenceScore legitScore, ReferenceScore cheatScore) {
        final double legitCombined = legitScore.combinedScore();
        final double cheatCombined = cheatScore.combinedScore();
        if (cheatCombined >= MIN_CLASSIFICATION_SCORE
                && cheatCombined - legitCombined >= MIN_CLASSIFICATION_MARGIN) {
            return new SampleDecision(Label.CHEAT_LIKE, legitCombined, cheatCombined);
        }
        if (legitCombined >= MIN_CLASSIFICATION_SCORE
                && legitCombined - cheatCombined >= MIN_CLASSIFICATION_MARGIN) {
            return new SampleDecision(Label.LEGIT_LIKE, legitCombined, cheatCombined);
        }
        return new SampleDecision(Label.UNCERTAIN, legitCombined, cheatCombined);
    }

    private ReferenceLibrary getReferenceLibrary() {
        ReferenceLibrary cached = referenceLibrary;
        if (cached != null) {
            return cached;
        }

        synchronized (AnalysisA.class) {
            cached = referenceLibrary;
            if (cached == null) {
                cached = ReferenceLibrary.load(sampleSize);
                referenceLibrary = cached;
            }
            return cached;
        }
    }

    private boolean shouldSkipDetection() {
        return player.packetStateData.lastPacketWasTeleport
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null;
    }

    private void writeRecordedSample(RecordedAttackSample sample) {
        final File recordedDirectory = ReferenceLibrary.locateDirectory(RECORDED_DIR);
        if (recordedDirectory == null) {
            return;
        }

        final File outFile = new File(recordedDirectory, sanitizeFileName(player.getName()) + ".json");
        try (FileWriter writer = new FileWriter(outFile, true)) {
            GSON.toJson(sample, writer);
            writer.write('\n');
        } catch (IOException exception) {
            LogUtil.error("Failed to write click jitter sample", exception);
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return player.uuid.toString();
        }
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void debug(String message) {
        if (!debugLog) {
            return;
        }
        final String formatted = "[AnalysisA] " + message;
        if (player.platformPlayer != null) {
            player.platformPlayer.sendMessage(formatted);
            return;
        }
        LogUtil.info("[AnalysisA] " + player.getName() + " " + message);
    }

    private void ensureRuntimeState() {
        if (tickBuffer == null) {
            tickBuffer = new ArrayDeque<>();
        }
        if (attacksAwaitingCenter == null) {
            attacksAwaitingCenter = new ArrayDeque<>();
        }
        if (pendingSamples == null) {
            pendingSamples = new ArrayDeque<>();
        }
        if (analysisWindow == null) {
            analysisWindow = new ArrayDeque<>();
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private enum Label {
        LEGIT_LIKE,
        CHEAT_LIKE,
        UNCERTAIN;

        private String logName() {
            return switch (this) {
                case LEGIT_LIKE -> "legit-like";
                case CHEAT_LIKE -> "cheat-like";
                case UNCERTAIN -> "uncertain";
            };
        }
    }

    private static final class RotationFrame {
        private final long sequence;
        private double deltaYaw;
        private double deltaPitch;

        private RotationFrame(long sequence, float deltaYaw, float deltaPitch) {
            this.sequence = sequence;
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
        }
    }

    private static final class PendingSample {
        private final long centerSequence;

        private PendingSample(long centerSequence) {
            this.centerSequence = centerSequence;
        }
    }

    private static final class SampleDecision {
        private final Label label;
        private final double legitScore;
        private final double cheatScore;

        private SampleDecision(Label label, double legitScore, double cheatScore) {
            this.label = label;
            this.legitScore = legitScore;
            this.cheatScore = cheatScore;
        }
    }

    private static final class ReferenceLibrary {
        private final List<ReferenceTemplate> legitTemplates;
        private final List<ReferenceTemplate> cheatTemplates;

        private ReferenceLibrary(List<ReferenceTemplate> legitTemplates, List<ReferenceTemplate> cheatTemplates) {
            this.legitTemplates = legitTemplates;
            this.cheatTemplates = cheatTemplates;
        }

        private static ReferenceLibrary load(int sampleSize) {
            final TemplateLoadResult legitLoadResult = loadTemplates(LEGIT_DIR, sampleSize, "legit");
            final TemplateLoadResult cheatLoadResult = loadTemplates(CHEAT_DIR, sampleSize, "cheat");
            locateDirectory(RECORDED_DIR);

            LogUtil.info(String.format(
                    "[AnalysisA] loaded legit files=%d templates=%d, cheat files=%d templates=%d",
                    legitLoadResult.fileCount,
                    legitLoadResult.templates.size(),
                    cheatLoadResult.fileCount,
                    cheatLoadResult.templates.size()
            ));
            return new ReferenceLibrary(legitLoadResult.templates, cheatLoadResult.templates);
        }

        private boolean hasTemplates() {
            return !legitTemplates.isEmpty() && !cheatTemplates.isEmpty();
        }

        private ReferenceScore score(WindowSignature signature, boolean legit) {
            final List<ReferenceTemplate> templates = legit ? legitTemplates : cheatTemplates;
            if (templates.isEmpty()) {
                return ReferenceScore.empty();
            }

            final List<Double> similarities = new ArrayList<>(templates.size());
            String bestReferenceName = "";
            double bestSimilarity = 0.0;
            for (ReferenceTemplate template : templates) {
                final double similarity = similarity(signature, template.signature);
                similarities.add(similarity);
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestReferenceName = template.referenceName;
                }
            }

            similarities.sort(Comparator.reverseOrder());
            final int topCount = Math.min(TOP_MATCH_COUNT, similarities.size());
            double topAverage = 0.0;
            for (int i = 0; i < topCount; i++) {
                topAverage += similarities.get(i);
            }
            if (topCount > 0) {
                topAverage /= topCount;
            }
            return new ReferenceScore(bestReferenceName, bestSimilarity, topAverage);
        }

        private double similarity(WindowSignature a, WindowSignature b) {
            final double yawAbsDistance = averageDistance(a.yawAbsNorm, b.yawAbsNorm);
            final double pitchAbsDistance = averageDistance(a.pitchAbsNorm, b.pitchAbsNorm);
            final double energyDistance = averageDistance(a.energyNorm, b.energyNorm);
            final double yawFlipDistance = averageFlipDistance(a.yawFlip, b.yawFlip);
            final double pitchFlipDistance = averageFlipDistance(a.pitchFlip, b.pitchFlip);
            final double yawSignDistance = averageSignDistance(a.yawSign, b.yawSign);
            final double pitchSignDistance = averageSignDistance(a.pitchSign, b.pitchSign);
            final double totalEnergyRatio = ratioDistance(a.totalEnergy, b.totalEnergy);
            final double peakEnergyRatio = ratioDistance(a.peakEnergy, b.peakEnergy);
            final double consistencyDistance = Math.abs(a.dominantConsistency - b.dominantConsistency);
            final double centerBandDistance = Math.abs(a.centerBandRatio - b.centerBandRatio);

            final double distance =
                    yawAbsDistance * 0.22
                            + pitchAbsDistance * 0.22
                            + energyDistance * 0.18
                            + yawSignDistance * 0.08
                            + pitchSignDistance * 0.08
                            + yawFlipDistance * 0.05
                            + pitchFlipDistance * 0.05
                            + totalEnergyRatio * 0.04
                            + peakEnergyRatio * 0.03
                            + consistencyDistance * 0.03
                            + centerBandDistance * 0.02;

            return Math.max(0.0, 1.0 - distance);
        }

        private static TemplateLoadResult loadTemplates(String directoryName, int sampleSize, String type) {
            final File directory = locateDirectory(directoryName);
            if (directory == null || !directory.isDirectory()) {
                return new TemplateLoadResult(Collections.emptyList(), 0);
            }

            final File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (files == null || files.length == 0) {
                return new TemplateLoadResult(Collections.emptyList(), 0);
            }

            final List<ReferenceTemplate> templates = new ArrayList<>();
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String trimmed = line.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }

                        try {
                            final RecordedAttackSample sample = GSON.fromJson(trimmed, RecordedAttackSample.class);
                            if (sample == null || sample.rotations == null || sample.rotations.size() != sampleSize) {
                                continue;
                            }

                            final WindowSignature signature = WindowSignature.fromRotations(sample.rotations, sampleSize / 2);
                            templates.add(new ReferenceTemplate(file.getName(), type, signature));
                        } catch (JsonSyntaxException ignored) {
                        }
                    }
                } catch (IOException ignored) {
                }
            }

            return new TemplateLoadResult(templates, files.length);
        }

        private static File locateDirectory(String childName) {
            final File dataFolder = EdGrimAPI.INSTANCE.getGrimPlugin().getDataFolder();
            final File root = new File(dataFolder, ROOT_DIR);
            if (!root.exists()) {
                root.mkdirs();
            }

            final File directory = new File(root, childName);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            return directory;
        }

        private static double averageDistance(double[] a, double[] b) {
            double sum = 0.0;
            for (int i = 0; i < a.length; i++) {
                sum += Math.abs(a[i] - b[i]);
            }
            return sum / a.length;
        }

        private static double averageFlipDistance(int[] a, int[] b) {
            if (a.length == 0) {
                return 0.0;
            }

            double sum = 0.0;
            for (int i = 0; i < a.length; i++) {
                sum += Math.abs(a[i] - b[i]);
            }
            return sum / a.length;
        }

        private static double averageSignDistance(int[] a, int[] b) {
            if (a.length == 0) {
                return 0.0;
            }

            double sum = 0.0;
            for (int i = 0; i < a.length; i++) {
                sum += Math.abs(a[i] - b[i]) / 2.0;
            }
            return sum / a.length;
        }

        private static double ratioDistance(double first, double second) {
            return Math.abs(first - second) / Math.max(Math.max(first, second), 1.0E-6);
        }
    }

    private static final class TemplateLoadResult {
        private final List<ReferenceTemplate> templates;
        private final int fileCount;

        private TemplateLoadResult(List<ReferenceTemplate> templates, int fileCount) {
            this.templates = templates;
            this.fileCount = fileCount;
        }
    }

    private static final class WindowSignature {
        private final double[] yawAbsNorm;
        private final double[] pitchAbsNorm;
        private final double[] energyNorm;
        private final int[] yawSign;
        private final int[] pitchSign;
        private final int[] yawFlip;
        private final int[] pitchFlip;
        private final double totalEnergy;
        private final double peakEnergy;
        private final double dominantConsistency;
        private final double centerBandRatio;

        private WindowSignature(
                double[] yawAbsNorm,
                double[] pitchAbsNorm,
                double[] energyNorm,
                int[] yawSign,
                int[] pitchSign,
                int[] yawFlip,
                int[] pitchFlip,
                double totalEnergy,
                double peakEnergy,
                double dominantConsistency,
                double centerBandRatio
        ) {
            this.yawAbsNorm = yawAbsNorm;
            this.pitchAbsNorm = pitchAbsNorm;
            this.energyNorm = energyNorm;
            this.yawSign = yawSign;
            this.pitchSign = pitchSign;
            this.yawFlip = yawFlip;
            this.pitchFlip = pitchFlip;
            this.totalEnergy = totalEnergy;
            this.peakEnergy = peakEnergy;
            this.dominantConsistency = dominantConsistency;
            this.centerBandRatio = centerBandRatio;
        }

        private static WindowSignature fromRotations(List<RecordedRotation> rotations, int centerIndex) {
            final int n = rotations.size();
            final double[] yawAbs = new double[n];
            final double[] pitchAbs = new double[n];
            final double[] energy = new double[n];
            final int[] yawSign = new int[n];
            final int[] pitchSign = new int[n];
            final int[] yawFlip = new int[Math.max(0, n - 1)];
            final int[] pitchFlip = new int[Math.max(0, n - 1)];

            double totalYawAbs = 0.0;
            double totalPitchAbs = 0.0;
            double totalEnergy = 0.0;
            double peakEnergy = 0.0;
            double signedYawSum = 0.0;
            double signedPitchSum = 0.0;

            for (int i = 0; i < n; i++) {
                final RecordedRotation rotation = rotations.get(i);
                yawAbs[i] = Math.abs(rotation.deltaYaw);
                pitchAbs[i] = Math.abs(rotation.deltaPitch);
                energy[i] = yawAbs[i] + pitchAbs[i];
                yawSign[i] = signOf(rotation.deltaYaw);
                pitchSign[i] = signOf(rotation.deltaPitch);
                peakEnergy = Math.max(peakEnergy, energy[i]);
                totalYawAbs += yawAbs[i];
                totalPitchAbs += pitchAbs[i];
                totalEnergy += energy[i];
                signedYawSum += rotation.deltaYaw;
                signedPitchSum += rotation.deltaPitch;
            }

            for (int i = 1; i < n; i++) {
                yawFlip[i - 1] = flipOf(yawSign[i - 1], yawSign[i]);
                pitchFlip[i - 1] = flipOf(pitchSign[i - 1], pitchSign[i]);
            }

            final double yawDenominator = Math.max(totalYawAbs, 1.0E-6);
            final double pitchDenominator = Math.max(totalPitchAbs, 1.0E-6);
            final double energyDenominator = Math.max(totalEnergy, 1.0E-6);
            final double dominantConsistency = Math.max(
                    Math.abs(signedYawSum) / yawDenominator,
                    Math.abs(signedPitchSum) / pitchDenominator
            );

            int centerStart = Math.max(0, centerIndex - 1);
            int centerEnd = Math.min(n - 1, centerIndex + 1);
            double centerBandEnergy = 0.0;
            for (int i = centerStart; i <= centerEnd; i++) {
                centerBandEnergy += energy[i];
            }

            final double[] yawAbsNorm = new double[n];
            final double[] pitchAbsNorm = new double[n];
            final double[] energyNorm = new double[n];
            for (int i = 0; i < n; i++) {
                yawAbsNorm[i] = yawAbs[i] / yawDenominator;
                pitchAbsNorm[i] = pitchAbs[i] / pitchDenominator;
                energyNorm[i] = energy[i] / energyDenominator;
            }

            return new WindowSignature(
                    yawAbsNorm,
                    pitchAbsNorm,
                    energyNorm,
                    yawSign,
                    pitchSign,
                    yawFlip,
                    pitchFlip,
                    totalEnergy,
                    peakEnergy,
                    dominantConsistency,
                    centerBandEnergy / energyDenominator
            );
        }

        private static int signOf(float value) {
            if (value > 1.0E-6F) {
                return 1;
            }
            if (value < -1.0E-6F) {
                return -1;
            }
            return 0;
        }

        private static int flipOf(int previous, int current) {
            if (previous == 0 || current == 0 || previous == current) {
                return 0;
            }
            return 1;
        }
    }

    private static final class ReferenceTemplate {
        private final String referenceName;
        @SuppressWarnings("unused")
        private final String referenceType;
        private final WindowSignature signature;

        private ReferenceTemplate(String referenceName, String referenceType, WindowSignature signature) {
            this.referenceName = referenceName;
            this.referenceType = referenceType;
            this.signature = signature;
        }
    }

    private static final class ReferenceScore {
        private final String bestReferenceName;
        private final double bestSimilarity;
        private final double topAverage;

        private ReferenceScore(String bestReferenceName, double bestSimilarity, double topAverage) {
            this.bestReferenceName = bestReferenceName;
            this.bestSimilarity = bestSimilarity;
            this.topAverage = topAverage;
        }

        private static ReferenceScore empty() {
            return new ReferenceScore("", 0.0, 0.0);
        }

        private double combinedScore() {
            return (bestSimilarity * 0.7) + (topAverage * 0.3);
        }
    }

    private static final class RecordedAttackSample {
        private final List<RecordedRotation> rotations;

        private RecordedAttackSample(List<RecordedRotation> rotations) {
            this.rotations = rotations;
        }
    }

    private static final class RecordedRotation {
        private final float deltaYaw;
        private final float deltaPitch;

        private RecordedRotation(float deltaYaw, float deltaPitch) {
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
        }
    }
}
