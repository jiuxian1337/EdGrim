package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.analysis.AnalysisMathUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;

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

    private int addGlobalVlIqr = 25;
    private int addGlobalVlBotPattern = 35;
    private int addGlobalVlZFactor = 0;
    private int addGlobalVlImprobable = 30;
    private int hitCancelIqrMs = 0;
    private int hitCancelBotPatternMs = 0;
    private int hitCancelZFactorMs = 5500;
    private int hitCancelImprobableMs = 4000;
    private float localVlLimitIqr = 11.0f;
    private float localVlLimitZFactor = 7.0f;
    private float localVlLimitImprobable = 15.0f;

    public AimStatistics(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        addGlobalVlIqr = clampAddGlobalVl(config.getIntElse("AimStatistics.add-global-vl-iqr", 25));
        addGlobalVlBotPattern = clampAddGlobalVl(config.getIntElse("AimStatistics.add-global-vl-bot-pattern", 25));
        addGlobalVlZFactor = clampAddGlobalVl(config.getIntElse("AimStatistics.add-global-vl-zfactor", 5));
        addGlobalVlImprobable = clampAddGlobalVl(config.getIntElse("AimStatistics.add-global-vl-improbable", 25));
        hitCancelIqrMs = config.getIntElse("AimStatistics.hit-cancel-time-ms-iqr", 0);
        hitCancelBotPatternMs = config.getIntElse("AimStatistics.hit-cancel-time-ms-bot-pattern", 0);
        hitCancelZFactorMs = config.getIntElse("AimStatistics.hit-cancel-time-ms-zfactor", 5500);
        hitCancelImprobableMs = config.getIntElse("AimStatistics.hit-cancel-time-ms-improbable", 4000);
        localVlLimitIqr = (float) config.getDoubleElse("AimStatistics.local-vl-limit-iqr", 11.0);
        localVlLimitZFactor = (float) config.getDoubleElse("AimStatistics.local-vl-limit-zfactor", 7.0);
        localVlLimitImprobable = (float) config.getDoubleElse("AimStatistics.local-vl-limit-improbable", 15.0);
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
        if (iqr > 12.5 && iqr < 96 && infs > 0) {
            iqrBuffer = Math.max(0.0f, iqrBuffer + ((iqr > 20) ? 1.4f : 0.8f));
            if (iqrBuffer > localVlLimitIqr) {
                if (addViolationsAndAlert(addGlobalVlIqr, "t=IQR iqr=" + iqr + " infs=" + infs + " buf=" + iqrBuffer)) {
                    mitigateDamage();
                }
                iqrBuffer = Math.max(0.0f, localVlLimitIqr - 2.0f);
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

        if (jiffPatterns > 2 && MathUtil.getAverage(yaw) > 3.0 && jiffPatterns != 6 && jiffPatterns != 12 && jiffPatterns != 4) {
            if (addViolationsAndAlert(addGlobalVlBotPattern, "t=BotPattern patterns=" + jiffPatterns)) {
                mitigateDamage();
            }
        }

        boolean positive = false;
        boolean negative = false;
        for (double d : zFactorYaw) {
            if (d > 10) positive = true;
            if (d < -10) negative = true;
        }

        if (zFactorYaw.size() == 2 && positive && negative && MathUtil.getMax(zFactorYaw) < 55) {
            zFactorBuffer = Math.max(0.0f, zFactorBuffer + 1.5f);
            if (zFactorBuffer > 4.0f) {
                total++;
            }
            if (zFactorBuffer > localVlLimitZFactor) {
                if (addViolationsAndAlert(addGlobalVlZFactor, "t=ZFactor values=" + zFactorYaw + " buf=" + zFactorBuffer)) {
                    mitigateDamage();
                }
                zFactorBuffer = Math.max(0.0f, localVlLimitZFactor - 1.0f);
            }
        } else {
            zFactorBuffer = Math.max(0.0f, zFactorBuffer - 1.2f);
        }

        if (total < 2.0f) {
            improbableBuffer = Math.max(0.0f, improbableBuffer - 2.0f);
        } else if (total > 2.0f) {
            improbableBuffer = Math.max(0.0f, improbableBuffer + 5.0f);
            if (improbableBuffer >= localVlLimitImprobable) {
                if (addViolationsAndAlert(addGlobalVlImprobable, "t=Improbable total=" + total + " buf=" + improbableBuffer)) {
                    mitigateDamage();
                }
                improbableBuffer = Math.max(0.0f, localVlLimitImprobable - 2.0f);
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
}
