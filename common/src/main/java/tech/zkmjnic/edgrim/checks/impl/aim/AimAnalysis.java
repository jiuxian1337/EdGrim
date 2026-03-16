package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.analysis.AnalysisMathUtil;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckData(name = "AimAnalysis", configName = "AimAnalysis", decay = 0.75, description = "MX aim analysis migrated")
public final class AimAnalysis extends EdAimCheck {
    private static final int SAMPLE_SIZE = 100;

    private final List<Float> rawYaw = new ArrayList<>(SAMPLE_SIZE);
    private final List<Float> rawPitch = new ArrayList<>(SAMPLE_SIZE);

    private final List<Float> limitedYaw = new ArrayList<>(SAMPLE_SIZE);
    private final List<Float> limitedPitch = new ArrayList<>(SAMPLE_SIZE);

    private final List<Float> longTermAnalysis = new ArrayList<>(10);
    private boolean linearQuery;
    private float rankBuffer;

    private boolean linearEnabled = true;
    private boolean rankEnabled = true;
    private boolean longtermEnabled = true;
    private float rankBufferLimit = 6.0f;

    public AimAnalysis(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        linearEnabled = config.getBooleanElse("AimAnalysis.enabled-linear", true);
        rankEnabled = config.getBooleanElse("AimAnalysis.enabled-rank", true);
        longtermEnabled = config.getBooleanElse("AimAnalysis.enabled-longterm", true);
        rankBufferLimit = (float) config.getDoubleElse("AimAnalysis.buffer-limit-rank", 6.0);
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
        if (rawYaw.size() >= SAMPLE_SIZE) {
            checkRaw();
        }

        if (Math.abs(deltaYaw) > 1.35f || (Math.abs(deltaPitch) > 1.35f && Math.abs(deltaYaw) > 0.32f)) {
            limitedYaw.add(deltaYaw);
            limitedPitch.add(deltaPitch);
            if (limitedYaw.size() >= SAMPLE_SIZE) {
                checkLimited();
            }
        }
    }

    private void checkLimited() {
        int resultDistinct = 0;
        final List<Float> yawStack = new ArrayList<>(10);
        for (final float yaw : limitedYaw) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDistinct += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final float distinctRank = (float) resultDistinct / 60.0f;
        longTermAnalysis.add(distinctRank);

        if (longTermAnalysis.size() >= 10) {
            final double avg = MathUtil.getAverage(longTermAnalysis);
            double normal = 0;
            for (double d : longTermAnalysis) {
                if (d > 0.97) normal++;
            }

            if (longtermEnabled && avg < 0.95 && normal < 4) {
                flagAnalysis("t=LongTerm avg=" + avg + " normal=" + normal + "/10");
            }
            longTermAnalysis.clear();
        }

        limitedYaw.clear();
        limitedPitch.clear();
    }

    private void checkRaw() {
        final int sens = calculateSensitivity();
        final List<Float> yawStack = new ArrayList<>(10);
        final List<Double> resultDeviation = new ArrayList<>();
        int resultDistinct = 0;

        for (final float yaw : rawYaw) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDeviation.add(MathUtil.getStandardDeviation(MathUtil.getJiffDelta(yawStack, 5)));
                resultDistinct += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        final List<Double> outliers = AnalysisMathUtil.zScoreOutliers(resultDeviation, 0.5);
        final float distinctRank = (float) resultDistinct / 60.0f;

        if (outliers.isEmpty() || (outliers.size() == 1 && Math.abs(outliers.get(0)) > 10 && Math.abs(outliers.get(0)) < 100)) {
            if (!linearQuery) {
                linearQuery = true;
            } else if (linearEnabled) {
                flagAnalysis("t=Linear outliers=" + Arrays.toString(outliers.toArray()));
            }
        } else {
            linearQuery = false;
        }

        final boolean valid = calculateSensitivity() > 20 && sens < 140;
        if (rankEnabled && distinctRank < 1.0f && distinctRank > 0.7f && MathUtil.getAverage(rawYaw) > 1.8 && valid) {
            if (rankBuffer < 0.01f) {
                if (distinctRank < 0.8f) rankBuffer += 0.2f;
            } else {
                final float inc = (distinctRank > 0.9f) ? 0.08f : (distinctRank > 0.8f) ? 2.0f : 3.0f;
                rankBuffer = Math.max(0.0f, rankBuffer + inc);
                if (rankBuffer >= rankBufferLimit) {
                    if (flagAnalysis("t=Rank rank=" + distinctRank + " buf=" + rankBuffer)) {
                        rankBuffer = Math.max(0.0f, rankBufferLimit - 1.0f);
                    }
                }
            }
        } else {
            rankBuffer = Math.max(0.0f, rankBuffer - 2.25f);
        }

        rawYaw.clear();
        rawPitch.clear();
    }

    private boolean flagAnalysis(String verbose) {
        if (flagAndAlert(verbose)) {
            mitigateDamage();
            return true;
        }
        return false;
    }
}
