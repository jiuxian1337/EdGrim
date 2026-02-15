package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.ExemptType;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayDeque;
import java.util.Deque;

@CheckData(name = "AnalysisB", configName = "AnalysisB", decay = 0.92, description = "Consistency-based analysis for robotic aim acceleration")
public class AnalysisB extends AnalysisCheck implements RotationCheck {
    private static final int WINDOW = 18;
    private static final double CONSISTENCY_THRESHOLD = 0.78;
    private static final double DYNAMIC_FACTOR = 1.18;
    private static final double DISCRETE_FACTOR = 0.82;
    private final Deque<Double> consistencyScores = new ArrayDeque<>();
    private final Deque<Double> discreteScores = new ArrayDeque<>();
    private long lastFlag;

    public AnalysisB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!isValidAttackState() || player.getTarget() == null || player.getTarget().type != EntityTypes.PLAYER || !shouldModifyPackets()) {
            resetWindow();
            return;
        }
        if (isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK, ExemptType.SERVER_SENT_ROTATE, ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE)) {
            resetWindow();
            buffer = 0;
            return;
        }
        AimProcessor processor = update.getProcessor();
        if (processor == null) {
            return;
        }
        double consistency = dynamicConsistency(processor);
        double discrete = enhancedDiscrete(processor);
        updateWindow(consistency, discrete);
        if (shouldFlag()) {
            if (time() - lastFlag < 500L) {
                return;
            }
            if (buffer++ > 4) {
                if (flagAndAlert(buildDebugMessage(consistency, discrete))) {
                    rewardBufferAndVL();
                }
                lastFlag = time();
            }
        } else {
            rewardBufferAndVL();
        }
    }

    private boolean isValidAttackState() {
        return hasAttackedSince(300L);
    }

    private void resetWindow() {
        consistencyScores.clear();
        discreteScores.clear();
    }

    private double dynamicConsistency(AimProcessor processor) {
        double deltaDotSum = Math.abs(processor.deltaDotsX) + Math.abs(processor.deltaDotsY);
        double frac = deltaDotSum - Math.floor(deltaDotSum);
        if (frac < 0.12 || frac > 0.88) {
            return 1.0;
        }
        return (frac > 0.35 && frac < 0.65) ? 0.4 : 0.0;
    }

    private double enhancedDiscrete(AimProcessor processor) {
        double yawAccel = processor.getYawAccel();
        double pitchAccel = processor.getPitchAccel();
        double c = Math.abs(yawAccel - pitchAccel);
        double d = Math.log1p(Math.abs(processor.getDeltaYaw()) + Math.abs(processor.getDeltaPitch()));
        return (c / (0.01 + (yawAccel + pitchAccel) / 2)) * d;
    }

    private void updateWindow(double consistency, double discrete) {
        consistencyScores.addLast(consistency);
        discreteScores.addLast(discrete);
        int pingFactor = Math.max(1, player.getTransactionPing() / 50);
        while (consistencyScores.size() > WINDOW * pingFactor) {
            consistencyScores.removeFirst();
            discreteScores.removeFirst();
        }
    }

    private boolean shouldFlag() {
        if (consistencyScores.size() < WINDOW / 2) {
            return false;
        }
        double cMean = AnalysisMathUtil.exponentialWeightedMean(consistencyScores);
        double dStd = MathUtil.stdDev(discreteScores, MathUtil.mean(discreteScores));
        double timeFactor = 1.0 - ((time() - player.actionManager.getLastAttack()) / 300.0);
        return cMean > (CONSISTENCY_THRESHOLD * (1.0 + timeFactor * DYNAMIC_FACTOR))
                && dStd < (DISCRETE_FACTOR * dStd + 0.15);
    }

    private String buildDebugMessage(double consistency, double discrete) {
        return String.format("C= %.2f (%.2f)\nD= %.2f \nWin= %d",
                consistency,
                AnalysisMathUtil.exponentialWeightedMean(consistencyScores),
                discrete,
                consistencyScores.size()
        );
    }
}
