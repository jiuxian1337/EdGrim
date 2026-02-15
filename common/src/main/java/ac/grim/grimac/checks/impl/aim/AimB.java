package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.lists.EvictingList;
import ac.grim.grimac.utils.math.MathUtil;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.List;

@CheckData(name = "AimB", configName = "AimB", decay = 0.75, setback = 7, description = "Statistical detection of robotic aim acceleration and jerk")
public final class AimB extends EdAimCheck {
    private final EvictingList<Double> yawAccelHistory;
    private final EvictingList<Double> pitchAccelHistory;
    private final EvictingList<Double> historicalKValues;
    private final EvictingList<Double> kHistory;
    private final EvictingList<Double> jerkHistory;
    private final EvictingList<Double> yawVarCache;
    private final EvictingList<Double> pitchVarCache;
    private long lastTick;
    private int violationLevel;
    private double kThreshold;
    private double lastSensitivityX;
    private double lastSensitivityY;
    private double jerkThreshold;

    public AimB(GrimPlayer player) {
        super(player);
        lastTick = -1;
        violationLevel = 0;
        yawAccelHistory = new EvictingList<>(50);
        pitchAccelHistory = new EvictingList<>(30);
        historicalKValues = new EvictingList<>(200);
        kHistory = new EvictingList<>(100);
        jerkHistory = new EvictingList<>(50);
        jerkThreshold = 8.0;
        kThreshold = 25.0;
        yawVarCache = new EvictingList<>(20);
        pitchVarCache = new EvictingList<>(20);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final long currentTick = rotationUpdate.getTick();

        if (yawAccelHistory.isEmpty() || pitchAccelHistory.isEmpty()) {
            resetHistory();
        }

        if (lastTick == -1 || currentTick != lastTick + 1 || !hasAttackedSince(1L)) {
            lastTick = currentTick;
            buffer *= 0.8;
            return;
        }

        if (!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return;
        }

        if (player.getTarget() == null) {
            return;
        }

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE) || player.getTarget().type != EntityTypes.PLAYER) {
            buffer *= 0.85;
            return;
        }

        double sensitivityX = rotationUpdate.getProcessor().sensitivityX;
        double sensitivityY = rotationUpdate.getProcessor().sensitivityY;
        if (sensitivityX != lastSensitivityX || sensitivityY != lastSensitivityY) {
            resetHistory();
            lastSensitivityX = sensitivityX;
            lastSensitivityY = sensitivityY;
        }

        yawAccelHistory.add((double) rotationUpdate.getProcessor().getYawAccel());
        pitchAccelHistory.add((double) rotationUpdate.getProcessor().getPitchAccel());

        if (rotationUpdate.getProcessor().getDeltaYaw() < 0.4 || rotationUpdate.getProcessor().getDeltaPitch() < 0.2) {
            lastTick = currentTick;
            return;
        }

        List<Double> yawDerivatives = MathUtil.computeDerivatives(yawAccelHistory);
        List<Double> pitchDerivatives = MathUtil.computeDerivatives(pitchAccelHistory);
        if (yawDerivatives.isEmpty() || pitchDerivatives.isEmpty()) return;

        double yawVar = MathUtil.getVariance(yawDerivatives);
        double pitchVar = MathUtil.getVariance(pitchDerivatives);
        yawVarCache.add(yawVar);
        pitchVarCache.add(pitchVar);

        double yawKurtosis = Math.abs(MathUtil.getKurtosis(yawAccelHistory));
        double pitchKurtosis = Math.abs(MathUtil.getKurtosis(pitchAccelHistory));
        double yawStd = Math.max(MathUtil.stdDev(yawAccelHistory), 0.001);
        double pitchStd = Math.max(MathUtil.stdDev(pitchAccelHistory), 0.001);

        double kValue = (Math.pow(yawKurtosis, 1.5) * 0.6 + Math.pow(pitchKurtosis, 1.2) * 0.4)
                * (1 / (Math.sqrt(yawStd) + 1e-6) + 1 / (Math.sqrt(pitchStd) + 1e-6));

        historicalKValues.add(kValue);
        kHistory.add(kValue);

        List<Double> kDerivatives = MathUtil.computeDerivatives(kHistory);
        double currentJerk = 0.0;
        if (kDerivatives.size() >= 2) {
            List<Double> jerkValues = MathUtil.computeDerivatives(kDerivatives);
            currentJerk = jerkValues.isEmpty() ? 0 : jerkValues.get(jerkValues.size() - 1);
            if (!jerkValues.isEmpty()) jerkHistory.add(currentJerk);
        }

        if (!historicalKValues.isEmpty()) {
            kThreshold = computeThreshold(historicalKValues);
        }
        if (!jerkHistory.isEmpty()) {
            jerkThreshold = MathUtil.computeJerkThreshold(jerkHistory);
        }

        boolean isSmoothCheat = checkPattern(kHistory, jerkHistory, yawVar, pitchVar);
        boolean isRoboticPattern = checkRoboticPattern(kDerivatives, jerkHistory);
        boolean isMachinePattern = (kValue > kThreshold) || isRoboticPattern;

        if (isMachinePattern) {
            if (kValue > 50 && Math.abs(currentJerk) > 20) {
                violationLevel += 30;
            } else if (kValue > kThreshold && Math.abs(currentJerk) > jerkThreshold) {
                violationLevel += 20;
            } else {
                violationLevel += 5;
            }
        } else {
            violationLevel = Math.max(0, violationLevel - 2);
        }

        if (violationLevel > 16) {
            double lastJerk = jerkHistory.isEmpty() ? 0 : jerkHistory.get(jerkHistory.size() - 1);
            if (flagAndAlert(String.format("K= %.1f\nJerk= %.1f\nYVar= %.1f\nPVar= %.1f",
                    kValue, lastJerk, yawVar, pitchVar))) {
                if (isAboveSetbackVl()) {
                    mitigateDamage();
                }
            }
            violationLevel = 0;
            resetHistory();
        }

        lastTick = currentTick;
    }

    private double calculateEMA(List<Double> data) {
        if (data.isEmpty()) return 0;
        double ema = data.get(0);
        for (int i = 1; i < data.size(); i++) {
            ema = 0.15 * data.get(i) + (1 - 0.15) * ema;
        }
        return ema;
    }

    private double computeThreshold(EvictingList<Double> history) {
        if (history.size() < 50) return 25.0;
        double emaMean = calculateEMA(history);
        double stdDev = MathUtil.stdDev(history);
        return Math.max(emaMean + 3.5 * stdDev, 30.0);
    }

    private boolean checkPattern(List<Double> kHistory, List<Double> jerkHistory, double yawVar, double pitchVar) {
        if (kHistory.size() < 50 || jerkHistory.size() < 30) return false;
        double kStd = MathUtil.stdDev(kHistory);
        double sum = 0;
        long count = 0;
        for (Double v : jerkHistory) {
            double abs = Math.abs(v);
            sum += abs;
            count++;
        }
        double jerkAvg = count > 0 ? sum / count : 0;
        long zeroCrossings = 0L;
        int bound = jerkHistory.size();
        for (int i = 1; i < bound; i++) {
            if (jerkHistory.get(i) * jerkHistory.get(i - 1) < 0) {
                zeroCrossings++;
            }
        }
        boolean lowVar = (yawVar < 15.0 && pitchVar < 15.0);
        return (kStd < 3.0 && jerkAvg < 1.5 && zeroCrossings < 8 && lowVar);
    }

    private boolean checkRoboticPattern(List<Double> kDerivatives, List<Double> jerkHistory) {
        if (kDerivatives.size() < 20 || jerkHistory.size() < 20) return false;
        double periodicity = MathUtil.calculatePeriodicity(kDerivatives);
        double jerkKurtosis = Math.abs(MathUtil.getKurtosis(jerkHistory));
        double autocorrelation = MathUtil.calculateAutocorrelation(kDerivatives, 3);
        return (periodicity > 0.8 && jerkKurtosis < 2.0 && autocorrelation > 0.6);
    }

    private void resetHistory() {
        yawAccelHistory.clear();
        pitchAccelHistory.clear();
        historicalKValues.clear();
        kHistory.clear();
        jerkHistory.clear();
        yawVarCache.clear();
        pitchVarCache.clear();
        kThreshold = 25.0;
        jerkThreshold = 8.0;
    }
}
