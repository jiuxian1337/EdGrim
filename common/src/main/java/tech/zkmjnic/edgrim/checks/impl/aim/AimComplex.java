package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.Tuple;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CheckData(name = "AimComplex", configName = "AimComplex", decay = 0.75, description = "MX complex aim checks migrated")
public final class AimComplex extends EdAimCheck {
    private static final int RAW_SAMPLE_SIZE = 10;
    private static final int SPIKE_SAMPLE_SIZE = 10;

    private final List<Float> rawYaw = new ArrayList<>(RAW_SAMPLE_SIZE);
    private final List<Float> rawPitch = new ArrayList<>(RAW_SAMPLE_SIZE);
    private final List<Integer> gcdYaw = new ArrayList<>(SPIKE_SAMPLE_SIZE);
    private final List<Integer> gcdPitch = new ArrayList<>(SPIKE_SAMPLE_SIZE);

    private double oldShannonYaw;
    private double oldShannonPitch;

    private final List<Double> kireikoGenericYaw = new ArrayList<>(7);
    private final List<Double> kireikoGenericPitch = new ArrayList<>(7);

    private float entropyPerfectBuffer;
    private float entropySimilarBuffer;
    private float distinctBuffer;
    private float randomizerBuffer;
    private float machineHeartBuffer;

    private boolean entropyEnabled = true;
    private boolean distinctEnabled = true;
    private boolean randomizerEnabled = true;
    private int entropyBufferLimit = 30;
    private float distinctBufferLimit = 4.0f;
    private float randomizerBufferLimit = 2.5f;

    public AimComplex(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        entropyEnabled = config.getBooleanElse("AimComplex.enabled-entropy", true);
        distinctEnabled = config.getBooleanElse("AimComplex.enabled-distinct", true);
        randomizerEnabled = config.getBooleanElse("AimComplex.enabled-randomizer", true);
        entropyBufferLimit = config.getIntElse("AimComplex.buffer-limit-entropy", 30);
        distinctBufferLimit = (float) config.getDoubleElse("AimComplex.buffer-limit-distinct", 4.0);
        randomizerBufferLimit = (float) config.getDoubleElse("AimComplex.buffer-limit-randomizer", 2.5);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (!hasAttackedSince(3500L)) return;
        if (rotationUpdate.isCinematic()) return;
        if (isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE, ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE, ExemptType.RESPAWN)) {
            return;
        }

        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        rawYaw.add(deltaYaw);
        rawPitch.add(deltaPitch);

        final double gcdValue = MathUtil.getGCDValueStatistic(0.5d) * 3.0;
        gcdYaw.add((int) (deltaYaw / gcdValue));
        gcdPitch.add((int) (deltaPitch / gcdValue));

        if (gcdYaw.size() >= SPIKE_SAMPLE_SIZE) {
            checkSpikes();
        }
        if (rawYaw.size() >= RAW_SAMPLE_SIZE) {
            checkRaw();
        }
    }

    private void checkRaw() {
        final int sens = calculateSensitivity();
        final int clientSens = player.checkManager.getRotationCheck(AimProcessor.class).totalSensitivityClient;
        final boolean valid = sens >= 60 && sens <= 150 && clientSens >= 60 && clientSens < 150;

        final int distinctYaw = getDistinct(rawYaw);
        final double shannonYaw = shannonEntropy(rawYaw);
        final double shannonPitch = shannonEntropy(rawPitch);

        if (entropyEnabled && valid && difference(shannonYaw, oldShannonYaw) < 1e-5 && difference(shannonPitch, oldShannonPitch) < 1e-5) {
            entropyPerfectBuffer = Math.max(0.0f, entropyPerfectBuffer + 1.0f);
            if (entropyPerfectBuffer > entropyBufferLimit) {
                if (flagComplex("t=EntropyPerfect yaw=" + shannonYaw + " pitch=" + shannonPitch + " sens=" + sens + " cs=" + clientSens)) {
                    entropyPerfectBuffer = Math.max(0.0f, entropyBufferLimit - 1.0f);
                }
            }
        } else {
            entropyPerfectBuffer = 0.0f;
        }

        if (entropyEnabled && valid && difference(shannonYaw, shannonPitch) < 1e-5) {
            entropySimilarBuffer = Math.max(0.0f, entropySimilarBuffer + 1.0f);
            if (entropySimilarBuffer > entropyBufferLimit) {
                if (flagComplex("t=EntropySimilar yaw=" + shannonYaw + " pitch=" + shannonPitch + " sens=" + sens + " cs=" + clientSens)) {
                    entropySimilarBuffer = Math.max(0.0f, entropyBufferLimit - 1.0f);
                }
            }
        } else {
            entropySimilarBuffer = 0.0f;
        }

        if (distinctEnabled && distinctYaw < 8 && Math.abs(MathUtil.getAverage(rawYaw)) > 2.5) {
            distinctBuffer = Math.max(0.0f, distinctBuffer + 1.7f);
            if (distinctBuffer >= distinctBufferLimit) {
                if (flagComplex("t=Distinct distinct=" + distinctYaw + " avg=" + MathUtil.getAverage(rawYaw) + " sens=" + sens + " cs=" + clientSens)) {
                    distinctBuffer = Math.max(0.0f, distinctBuffer - 0.5f);
                }
            }
        } else {
            distinctBuffer = Math.max(0.0f, distinctBuffer - 0.35f);
        }

        oldShannonYaw = shannonYaw;
        oldShannonPitch = shannonPitch;

        rawYaw.clear();
        rawPitch.clear();
    }

    private void checkSpikes() {
        if (gcdYaw.isEmpty()) {
            gcdYaw.clear();
            gcdPitch.clear();
            return;
        }

        Tuple<List<Double>, List<Double>> yawOutliers = MathUtil.getOutliers(gcdYaw);
        Tuple<List<Double>, List<Double>> pitchOutliers = MathUtil.getOutliers(gcdPitch);

        final double kireikoYaw = getKireikoGeneric(gcdYaw);
        final double kireikoPitch = getKireikoGeneric(gcdPitch);

        kireikoGenericYaw.add(kireikoYaw);
        kireikoGenericPitch.add(kireikoPitch);

        if (kireikoGenericYaw.size() >= 7) {
            double xDev = MathUtil.getStandardDeviation(kireikoGenericYaw);
            double xSpikeMax = MathUtil.getMax(kireikoGenericYaw);

            if (xDev > 5 && xDev < 22 && xSpikeMax < 50) {
                float inc = (MathUtil.getAverage(kireikoGenericYaw) < 6.0) ? 0.0f : (xDev < 10) ? 1.5f : 1.0f;
                machineHeartBuffer = Math.max(0.0f, machineHeartBuffer + inc);
                if (machineHeartBuffer >= 7.0f) {
                    machineHeartBuffer = 6.0f;
                }
            } else {
                float dec = (xDev < 40 || xSpikeMax < 70) ? -0.4f : -0.8f;
                machineHeartBuffer = Math.max(0.0f, machineHeartBuffer + dec);
            }
            kireikoGenericYaw.clear();
            kireikoGenericPitch.clear();
        }

        double devYaw = MathUtil.getVariance(gcdYaw);
        double devPitch = MathUtil.getVariance(gcdPitch);
        double min = Math.min(devYaw, devPitch);
        double max = Math.max(devYaw, devPitch);

        if (randomizerEnabled && (min < 0.09 && max > 35 && MathUtil.getMin(gcdPitch) != 0.0) && calculateSensitivity() > 50) {
            randomizerBuffer = Math.max(0.0f, randomizerBuffer + 1.0f);
            if (randomizerBuffer > randomizerBufferLimit) {
                if (flagComplex("t=RandomizerFlaw varYaw=" + devYaw + " varPitch=" + devPitch)) {
                    randomizerBuffer = Math.max(0.0f, randomizerBufferLimit - 1.0f);
                }
            }
        } else {
            randomizerBuffer = Math.max(0.0f, randomizerBuffer - 0.4f);
        }

        gcdYaw.clear();
        gcdPitch.clear();
    }

    private static double difference(double a, double b) {
        return Math.abs(Math.abs(a) - Math.abs(b));
    }

    private static int getDistinct(List<Float> values) {
        Set<Float> set = new HashSet<>(values);
        return set.size();
    }

    private static double shannonEntropy(List<Float> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        Map<Float, Integer> freq = new HashMap<>();
        for (float v : values) {
            freq.merge(v, 1, Integer::sum);
        }
        double total = values.size();
        double entropy = 0.0;
        for (int count : freq.values()) {
            double p = count / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    private static double getKireikoGeneric(final List<Integer> values) {
        return (MathUtil.getKurtosis(values) + (MathUtil.getVariance(values) * 3.0)) / 20.0;
    }

    private boolean flagComplex(String verbose) {
        if (flagAndAlert(verbose)) {
            mitigateDamage();
            return true;
        }
        return false;
    }
}
