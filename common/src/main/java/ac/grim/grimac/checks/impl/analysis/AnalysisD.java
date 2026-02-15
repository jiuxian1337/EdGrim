package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.ExemptType;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;
import ac.grim.grimac.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CheckData(name = "AnalysisD", configName = "AnalysisD", decay = 0.92, description = "Rotation distribution and outlier analysis")
public final class AnalysisD extends AnalysisCheck implements RotationCheck {
    private static final int RAW_ROTATIONS_THRESHOLD = 100;
    private static final int SCORE_CHUNK_SIZE = 10;
    private final List<Vec2f> rawRotations = new ArrayList<>();
    private final List<Vec2f> limitedRotations = new ArrayList<>();
    private final List<Float> longTermAnalysis = new ArrayList<>();
    private double scoreBuffer;
    private double distributionBuffer;
    private double limitBuffer;
    private double outlierBuffer;
    private boolean query;

    public AnalysisD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!isInCombat() || hasExemptions() || !shouldModifyPackets()) {
            reduceAllBuffers(0.80, 0.3);
            rawRotations.clear();
            return;
        }

        if (player.getTarget() == null) {
            return;
        }

        if (!player.isMoving()) {
            return;
        }

        Vec2f delta = update.getDelta();
        rawRotations.add(delta);

        if (rawRotations.size() >= RAW_ROTATIONS_THRESHOLD) {
            checkRotationData();
        }

        int sens = player.calculateSensitivity();
        if (sens > 50 && (Math.abs(delta.getX()) > 1.35 || (Math.abs(delta.getY()) > 1.35 && Math.abs(delta.getX()) > 0.32))) {
            limitedRotations.add(delta);
            if (limitedRotations.size() >= 100) {
                checkLimited();
            }
        }
    }

    private void checkRotationData() {
        List<Float> yawChanges = new ArrayList<>(rawRotations.size());
        List<Float> pitchChanges = new ArrayList<>(rawRotations.size());
        List<Long> xGcd = new ArrayList<>(rawRotations.size());

        int sens = player.calculateSensitivity();
        double gcdValue = sens > 0 ? MathUtil.getGCDValueStatistic(sens / 200.0) : 0.0;

        for (Vec2f vec : rawRotations) {
            yawChanges.add(vec.getX());
            pitchChanges.add(vec.getY());
            if (gcdValue != 0.0) {
                xGcd.add((long) (vec.getX() / gcdValue));
            }
        }

        checkScore(yawChanges);
        checkDistribution(yawChanges, pitchChanges);

        rawRotations.clear();
    }

    private void checkScore(List<Float> yawChanges) {
        List<Float> yawStack = new ArrayList<>();
        List<Double> deviations = new ArrayList<>();
        int distinctCount = 0;

        for (float yaw : yawChanges) {
            yawStack.add(yaw);
            if (yawStack.size() >= SCORE_CHUNK_SIZE) {
                deviations.add(MathUtil.stdDev(MathUtil.getJiffDelta(yawStack, 5)));
                distinctCount += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        List<Double> outliers = AnalysisMathUtil.zScoreOutliers(deviations, 0.5f);
        float distinctRank = (float) distinctCount / 60;
        if (outliers.isEmpty()) {
            return;
        }
        if (outliers.size() == 1 && Math.abs(outliers.get(0)) > 18 && Math.abs(outliers.get(0)) < 100) {
            if (!query) {
                query = true;
            } else {
                if (++outlierBuffer > 5) {
                    if (flagAndAlert("o= " + Arrays.toString(outliers.toArray()))) {
                        mitigateDamage();
                    }
                }
            }
        } else {
            query = false;
            outlierBuffer = Math.max(0, outlierBuffer - 0.5);
        }

        int sens = player.calculateSensitivity();
        boolean valid = sens > 20 && sens < 140;
        if (distinctRank < 1.0 && distinctRank > 0.7 && MathUtil.getAverage(yawChanges) > 1.8 && valid) {
            if (++scoreBuffer > 3) {
                if (flagAndAlert("r= " + distinctRank)) {
                    if (scoreBuffer > 5.6) {
                        mitigateDamage();
                    }
                }
            }
        } else {
            scoreBuffer = Math.max(0, scoreBuffer - 0.5);
        }
    }

    private void checkLimited() {
        List<Float> x = new ArrayList<>();
        for (Vec2f vec2 : limitedRotations) {
            x.add(vec2.getX());
        }

        List<Float> yawStack = new ArrayList<>();
        int resultDistinct = 0;
        for (float yaw : x) {
            yawStack.add(yaw);
            if (yawStack.size() >= 10) {
                resultDistinct += MathUtil.getDistinct(MathUtil.getJiffDelta(yawStack, 4));
                yawStack.clear();
            }
        }

        float distinctRank = (float) resultDistinct / 60;
        longTermAnalysis.add(distinctRank);

        if (longTermAnalysis.size() >= 10) {
            double avg = MathUtil.getAverage(longTermAnalysis);
            long normalCount = longTermAnalysis.stream().filter(d -> d > 0.97).count();

            if (avg < 0.95 && normalCount < 4) {
                if (limitBuffer++ > 4) {
                    if (flagAndAlert("(Limit)\navg= " + avg + "\nn= " + normalCount)) {
                        mitigateDamage();
                    }
                }
            } else {
                limitBuffer = Math.max(0, limitBuffer - 0.75);
            }

            longTermAnalysis.clear();
        }

        limitedRotations.clear();
    }

    private void checkDistribution(List<Float> yawChanges, List<Float> pitchChanges) {
        double distinctX = MathUtil.getDistinct(yawChanges);
        double maxYawAbs = Math.abs(MathUtil.getMax(yawChanges));
        double kurtosis = MathUtil.getKurtosis(yawChanges);
        double pearson = AnalysisMathUtil.pearsonCorrelation(yawChanges, pitchChanges);
        int spikeCount = AnalysisMathUtil.zScoreOutliers(yawChanges, 1.0f).size() + AnalysisMathUtil.zScoreOutliers(pitchChanges, 1.0f).size();

        if (maxYawAbs > 8 && pearson < 0.25 && distinctX < 85 && distinctX > 65 && kurtosis > 0 && spikeCount >= 40) {
            if (++distributionBuffer > 4) {
                if (flagAndAlert("d= " + distinctX + "\np= " + pearson + "\nmax= " + maxYawAbs + "\ns= " + spikeCount)) {
                    mitigateDamage();
                }
            }
        } else {
            distributionBuffer = Math.max(0, distributionBuffer - 0.5);
        }
    }

    private void reduceAllBuffers(double factor, double minCutoff) {
        scoreBuffer *= factor;
        distributionBuffer *= factor;
        outlierBuffer *= factor;
        limitBuffer *= factor;
        if (limitBuffer < minCutoff) {
            limitBuffer = 0;
        }
        if (scoreBuffer < minCutoff) {
            scoreBuffer = 0;
        }
        if (outlierBuffer < minCutoff) {
            outlierBuffer = 0;
        }
        if (distributionBuffer < minCutoff) {
            distributionBuffer = 0;
        }
    }

    private boolean isInCombat() {
        return hasAttackedSince(1500L);
    }

    private boolean hasExemptions() {
        if (player.getTarget() == null) {
            return true;
        }
        return isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE, ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE)
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.getTarget().type != EntityTypes.PLAYER
                || (player.getLastTarget() != null && !player.getTarget().getUuid().equals(player.getLastTarget().getUuid()));
    }
}
