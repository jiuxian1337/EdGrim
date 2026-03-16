package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.analysis.AnalysisMathUtil;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@CheckData(name = "AimStatistics", configName = "AimStatistics", decay = 0.75, description = "MX aim statistics migrated")
public final class AimStatistics extends EdAimCheck {
    private static final int SAMPLE_SIZE = 25;

    private final List<Float> yaw = new ArrayList<>(SAMPLE_SIZE);
    private final List<Float> pitch = new ArrayList<>(SAMPLE_SIZE);
    private final List<Double> shannonAnalysis = new ArrayList<>(10);

    private float iqrBuffer;
    private float zFactorBuffer;
    private float improbableBuffer;

    private boolean iqrEnabled = true;
    private boolean botPatternEnabled = true;
    private boolean zFactorEnabled = true;
    private boolean improbableEnabled = true;
    private float iqrBufferLimit = 11.0f;
    private float zFactorBufferLimit = 7.0f;
    private float improbableBufferLimit = 15.0f;

    public AimStatistics(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        iqrEnabled = config.getBooleanElse("AimStatistics.enabled-iqr", true);
        botPatternEnabled = config.getBooleanElse("AimStatistics.enabled-bot-pattern", true);
        zFactorEnabled = config.getBooleanElse("AimStatistics.enabled-zfactor", true);
        improbableEnabled = config.getBooleanElse("AimStatistics.enabled-improbable", true);
        iqrBufferLimit = (float) config.getDoubleElse("AimStatistics.buffer-limit-iqr", 11.0);
        zFactorBufferLimit = (float) config.getDoubleElse("AimStatistics.buffer-limit-zfactor", 7.0);
        improbableBufferLimit = (float) config.getDoubleElse("AimStatistics.buffer-limit-improbable", 15.0);
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
        yaw.add(deltaYaw);
        pitch.add(deltaPitch);
        if (yaw.size() >= SAMPLE_SIZE) {
            checkSample();
        }
    }

    private void checkSample() {
        float total = 0.0f;

        final List<Double> zFactorYaw = AnalysisMathUtil.zScoreOutliers(yaw, 2.0);
        final List<Float> jiffYaw = MathUtil.getJiffDelta(yaw, 5);
        final List<Float> jiffPitch = MathUtil.getJiffDelta(pitch, 5);

        final int omniSize = Math.min(jiffYaw.size(), jiffPitch.size());
        final List<Float> jiffOmni = new ArrayList<>(omniSize);
        for (int i = 0; i < omniSize; i++) {
            jiffOmni.add(jiffYaw.get(i) / jiffPitch.get(i));
        }

        int infs = 0;
        for (float j : jiffOmni) {
            if (Float.isInfinite(j)) infs++;
        }

        final double iqr = MathUtil.getIQR(jiffOmni);
        if (iqrEnabled && iqr > 12.5 && iqr < 96 && infs > 0) {
            iqrBuffer = Math.max(0.0f, iqrBuffer + ((iqr > 20) ? 1.4f : 0.8f));
            if (iqrBuffer > iqrBufferLimit) {
                if (flagStatistics("t=IQR iqr=" + iqr + " infs=" + infs + " buf=" + iqrBuffer)) {
                    iqrBuffer = Math.max(0.0f, iqrBufferLimit - 2.0f);
                }
            }
        } else if (iqr < 13 || infs == 0) {
            iqrBuffer = Math.max(0.0f, iqrBuffer - ((iqr < 7) ? 5.0f : 3.5f));
        }

        final double kTest = MathUtil.kolmogorovSmirnovTest(MathUtil.getJiffDelta(yaw, 6), Function.identity());
        if (kTest > 10 && Math.abs(MathUtil.getAverage(yaw)) < 13) {
            total++;
        }

        shannonAnalysis.add(shannonEntropy(jiffYaw));
        if (shannonAnalysis.size() > 9) {
            Set<Double> uniq = new HashSet<>(shannonAnalysis);
            double diff = Math.abs(Math.abs(MathUtil.getMin(uniq)) - Math.abs(MathUtil.getMax(uniq)));
            shannonAnalysis.clear();
        }

        int jiffPatterns = 0;
        for (int i = 0; i < jiffYaw.size(); i++) {
            float f = jiffYaw.get(i);
            if (!String.valueOf(f).contains("E") || f == 0.0f) continue;
            for (int r = 0; r < jiffYaw.size(); r++) {
                if (r == i) continue;
                if (f == jiffYaw.get(r)) {
                    jiffPatterns++;
                }
            }
        }

        if (botPatternEnabled && jiffPatterns > 2 && MathUtil.getAverage(yaw) > 3.0 && jiffPatterns != 6 && jiffPatterns != 12 && jiffPatterns != 4) {
            flagStatistics("t=BotPattern patterns=" + jiffPatterns);
        }

        boolean positive = false;
        boolean negative = false;
        for (double d : zFactorYaw) {
            if (d > 10) positive = true;
            if (d < -10) negative = true;
        }

        if (zFactorEnabled && zFactorYaw.size() == 2 && positive && negative && MathUtil.getMax(zFactorYaw) < 55) {
            zFactorBuffer = Math.max(0.0f, zFactorBuffer + 1.5f);
            if (zFactorBuffer > 4.0f) {
                total++;
            }
            if (zFactorBuffer > zFactorBufferLimit) {
                if (flagStatistics("t=ZFactor values=" + zFactorYaw + " buf=" + zFactorBuffer)) {
                    zFactorBuffer = Math.max(0.0f, zFactorBufferLimit - 1.0f);
                }
            }
        } else {
            zFactorBuffer = Math.max(0.0f, zFactorBuffer - 1.2f);
        }

        if (total < 2.0f) {
            improbableBuffer = Math.max(0.0f, improbableBuffer - 2.0f);
        } else if (improbableEnabled && total >= 2.0f) {
            improbableBuffer = Math.max(0.0f, improbableBuffer + 5.0f);
            if (improbableBuffer >= improbableBufferLimit) {
                if (flagStatistics("t=Improbable total=" + total + " buf=" + improbableBuffer)) {
                    improbableBuffer = Math.max(0.0f, improbableBufferLimit - 2.0f);
                }
            }
        }

        yaw.clear();
        pitch.clear();
    }

    private static double shannonEntropy(List<Float> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = max - min;
        if (range == 0.0) {
            return 0.0;
        }
        int bins = 12;
        int[] counts = new int[bins];
        double binSize = range / bins;
        for (float v : values) {
            int idx = (int) ((v - min) / binSize);
            if (idx >= bins) idx = bins - 1;
            counts[idx]++;
        }
        double entropy = 0.0;
        int total = values.size();
        for (int c : counts) {
            if (c > 0) {
                double p = (double) c / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    private boolean flagStatistics(String verbose) {
        if (flagAndAlert(verbose)) {
            mitigateDamage();
            return true;
        }
        return false;
    }
}
