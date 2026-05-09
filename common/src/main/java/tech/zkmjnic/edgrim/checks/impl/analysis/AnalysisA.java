package tech.zkmjnic.edgrim.checks.impl.analysis;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.analysis.a.*;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@CheckData(
        name = "AnalysisA",
        description = "attack-centered heuristic analysis"
)
public final class AnalysisA extends Check implements RotationCheck {

    public static final Gson GSON = new GsonBuilder().create();
    public static final int TOP_MATCH_COUNT = 3;
    public static final String ROOT_DIR = "analysis-a";
    public static final String LEGIT_DIR = "legit";
    public static final String CHEAT_DIR = "cheat";
    public static final String RECORDED_DIR = "recorded";
    static final int MAX_BUFFERED_UPDATES = 64;
    static final double MIN_SAMPLE_ENERGY = 1.25;
    static final double MIN_CLASSIFICATION_SCORE = 0.86;
    static final double MIN_CLASSIFICATION_MARGIN = 0.012;
    static volatile ReferenceLibrary referenceLibrary;

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

    public static void reloadGlobal(ConfigManager config) {
        int sampleRadius = Math.max(1, config.getIntElse("AnalysisA.sample-radius", 5));
        int sampleSize = (sampleRadius * 2) + 1;
        referenceLibrary = ReferenceLibrary.load(sampleSize);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
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
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (!shouldModifyPackets()) return;
            if (!referenceLibrary.hasTemplates()) return;
            ensureRuntimeState();
            if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
                return;
            }

            final WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }

            attacksAwaitingCenter.addLast(rotationSequence + 1L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(RotationUpdate update) {
        try {
            if (!shouldModifyPackets()) return;
            if (!referenceLibrary.hasTemplates()) return;
            ensureRuntimeState();
            if (update.isCinematic2()) {
                return;
            }

            appendRotation(update);
            resolveAttackCenters();
            drainPendingSamples();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            if (rotationSequence < pending.centerSequence() + sampleRadius) {
                return;
            }

            pendingSamples.removeFirst();
            final RecordedAttackSample sample = extractSample(pending.centerSequence());
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
        if (library == null || !library.hasTemplates()) {
            debug("sample skipped because legit/cheat template library is not ready");
            return;
        }

        final WindowSignature signature = WindowSignature.fromRotations(sample.rotations(), sampleRadius);
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
                decision.label().logName(),
                decision.legitScore(),
                decision.cheatScore(),
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
            if (decision.label() == Label.CHEAT_LIKE) {
                cheatVotes++;
            } else if (decision.label() == Label.LEGIT_LIKE) {
                legitVotes++;
            } else {
                uncertainVotes++;
            }
            totalCheatScore += decision.cheatScore();
            totalLegitScore += decision.legitScore();
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
        return referenceLibrary;
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
            LogUtil.error("Failed to write heuristic analysis sample", exception);
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
}
