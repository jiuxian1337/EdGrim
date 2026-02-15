package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.aim.ExemptType;
import ac.grim.grimac.checks.impl.aim.processor.AimProcessor;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;
import ac.grim.grimac.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CheckData(name = "AnalysisC", configName = "AnalysisC", decay = 0.85, description = "Entropy and kurtosis based combat rotation analysis")
public class AnalysisC extends AnalysisCheck implements RotationCheck {
    private static final long COMBAT_COOLDOWN_MS = 100;
    private final List<Double> kurtosisHistory = new ArrayList<>();
    private final List<Float> yawEntropyHist = new ArrayList<>();
    private final List<Float> pitchEntropyHist = new ArrayList<>();
    private final List<Integer> dYHistory = new ArrayList<>(10);
    private final List<Vec2f> rotation = new ArrayList<>();
    private int counter;
    private long lastFlag;
    private long lastFlag2;
    private long lastFlag3;
    private long lastFlag4;
    private double buffer2;
    private double buffer3;
    private double buffer4;
    private double buffer5;
    private List<Vec2f> rotations;
    private long lastCombatEndTime;
    private long lastCombatTime;
    private long lastAnalyze;

    public AnalysisC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!hasAttackedSince(1400L)) {
            rotation.clear();
            return;
        }
        if (player.getTarget() == null) {
            rotation.clear();
            return;
        }
        if (player.getTarget().type != EntityTypes.PLAYER) {
            return;
        }
        if (Math.abs(update.getTo().getPitch()) >= 89.9F) {
            return;
        }
        if (isExempt(ExemptType.TELEPORT, ExemptType.VEHICLE, ExemptType.SERVER_SENT_PULLBACK, ExemptType.ELYTRA_FLYING)) {
            return;
        }
        rotation.add(update.getDelta());
        if (player.calculateSensitivity() < 40) {
            rotation.clear();
            rotations = null;
            return;
        }
        if (rotation.size() > 50) {
            rotations = rotation;
            checkForAimbot(update.getProcessor(), update);
        }
    }

    private void checkForAimbot(AimProcessor processor, RotationUpdate update) {
        if (rotations == null) {
            return;
        }
        if (rotations.size() < 80 || time() - lastAnalyze < 400) {
            return;
        }
        lastAnalyze = time();
        if (processor == null) {
            return;
        }
        List<Float> yaws = new ArrayList<>(rotations.size());
        List<Float> pitches = new ArrayList<>(rotations.size());
        for (Vec2f rot : rotations) {
            yaws.add(rot.getX());
            pitches.add(rot.getY());
        }
        double avgYaw = MathUtil.getAverage(yaws);
        double avgPitch = MathUtil.getAverage(pitches);
        double maxYaw = maxAbs(yaws);
        double maxPitch = maxAbs(pitches);
        double sens = player.calculateSensitivity();
        if (sens < 35) {
            rotations.clear();
            return;
        }
        checkBasicPatterns(avgYaw);
        if (rotations.size() > 50) {
            checkDKS(update, yaws, pitches, avgYaw, maxYaw, avgPitch, maxPitch);
            if (update.isCinematic()) {
                rotations.clear();
                return;
            }
            if (sens > 48) {
                checkEntropyPatterns(yaws, pitches, avgYaw, avgPitch);
                checkAdvancedPatterns(yaws, avgYaw);
            }
        }
    }

    private double avgVar(List<Float> hist, double current) {
        double sum = 0.0;
        long count = 0;
        for (Float v : hist) {
            sum += Math.abs(v - current);
            count++;
        }
        return count > 0 ? sum / count : 0.0;
    }

    private void checkEntropyPatterns(List<Float> yaws, List<Float> pitches, double avgYaw, double avgPitch) {
        double yawEnt = MathUtil.calculateNEntropy(yaws);
        double pitchEnt = MathUtil.calculateNEntropy(pitches);
        double similarScore = Math.abs(yawEnt - pitchEnt);
        yawEntropyHist.add((float) yawEnt);
        pitchEntropyHist.add((float) pitchEnt);
        int historySize = 7;
        if (yawEntropyHist.size() > historySize) {
            yawEntropyHist.remove(0);
            pitchEntropyHist.remove(0);
        }
        if (yawEntropyHist.size() >= historySize) {
            double yawVar = avgVar(yawEntropyHist, yawEnt);
            double pitchVar = avgVar(pitchEntropyHist, pitchEnt);
            boolean suspicious = false;
            String reason = "";
            if (similarScore > 3) {
                suspicious = true;
                reason = String.format("[H] ss= %.3f", similarScore);
            } else if (similarScore < 0.01 && similarScore != 0 && avgPitch > 1 && avgYaw > 1) {
                suspicious = true;
                reason = String.format("[L] ss= %.3f", similarScore);
            } else if (((yawEnt < 1.0 && pitchEnt > 2.0) || (pitchEnt < 1.0 && yawEnt > 2.0))
                    && avgYaw > 2 && avgPitch > 2 && (yawEnt >= 0.01 && pitchEnt >= 0.01)) {
                suspicious = true;
                reason = String.format("[LOS] y= %.3f\np= %.3f", yawEnt, pitchEnt);
            } else if (yawEnt < 0.5 && pitchEnt < 0.5 && yawVar < 0.1 && pitchVar < 0.1 && avgYaw > 2 && avgPitch > 2
                    && (yawEnt >= 0.01 && pitchEnt >= 0.01)) {
                suspicious = true;
                reason = String.format("[BLS] y=%.3f\np= %.3f", yawEnt, pitchEnt);
            }
            if (suspicious) {
                buffer5 = modifyBuffer(buffer5, 1.0);
                if (buffer5 > 4) {
                    if (flagAndAlert("(Entropy)\nMain= " + reason + "\ny= " + yawEnt + "\npe= " + pitchEnt + "\nyv= " + yawVar + "\npv= " + pitchVar)) {
                        mitigateDamage();
                        buffer5 = decayBuffer(buffer5, 0.95, 1.0);
                    }
                }
            } else {
                buffer5 = decayBuffer(buffer5, 0.85, 1.5);
            }
        }
    }

    private void checkBasicPatterns(double avgYaw) {
        double var = variation();
        double cons = consistency();
        if (var < 0.08 && avgYaw > 1) {
            if (time() - lastFlag < 800L) {
                return;
            }
            buffer3 = modifyBuffer(buffer3, 1.0);
            if (buffer3 > 4) {
                if (flagAndAlert("(Basic)\nvar= " + var)) {
                    mitigateDamage();
                    buffer3 = decayBuffer(buffer3, 0.75, 1.0);
                }
            }
            lastFlag = time();
        } else {
            buffer3 = decayBuffer(buffer3, 0.85, 2.0);
        }
        if (cons > 0.85) {
            if (time() - lastFlag2 < 500L) {
                return;
            }
            buffer4 = modifyBuffer(buffer4, 1.0);
            if (buffer4 > 4) {
                if (flagAndAlert("(Basic)\nc= " + cons)) {
                    mitigateDamage();
                    buffer4 = decayBuffer(buffer4, 0.75, 1.5);
                }
            }
            lastFlag2 = time();
        } else {
            buffer4 = decayBuffer(buffer4, 0.95, 2.0);
        }
    }

    private boolean isLegitPattern(int dY, double kurtosis, double pearson, int spikes) {
        if (dY > 30 && dY < 80 && Math.abs(kurtosis) < 3.0 && pearson > 0.1 && pearson < 0.7) {
            return true;
        }
        if (kurtosis > 15 && kurtosisHistory.stream().noneMatch(k -> k > 10) && dY > 20 && spikes < 15) {
            return true;
        }
        if (!dYHistory.isEmpty()) {
            double growthRate = (double) dY / dYHistory.get(dYHistory.size() - 1);
            return growthRate > 1.2 && growthRate < 2.5 && dY > 15 && kurtosis < 5.0;
        }
        return false;
    }

    private void checkDKS(RotationUpdate update, List<Float> yaws, List<Float> pitches, double avgYaw, double maxYaw, double avgPitch, double maxPitch) {
        if (time() - lastCombatEndTime < COMBAT_COOLDOWN_MS) {
            return;
        }
        int combatStarts = counter;
        int distinctYaw = distinct(yaws);
        int spikes = detectOutliers(yaws).size() + detectOutliers(pitches).size();
        double kurtosis = kurtosis(yaws);
        double pearson = AnalysisMathUtil.pearsonCorrelation(yaws, pitches);
        if (dYHistory.size() >= 10) {
            dYHistory.remove(0);
        }
        dYHistory.add(distinctYaw);
        double stillnessDuration = calculateStillnessDuration(update);
        double movementAfterStillness = calculateMovementAfterStillness();
        boolean legit = isLegitPattern(distinctYaw, kurtosis, pearson, spikes);
        boolean suspicious = false;
        String patternType = "Normal";
        if (!legit) {
            if (distinctYaw < 20 && kurtosis > 15 && pearson < 0.5 && avgYaw > 0.5) {
                suspicious = combatStarts > 1;
                patternType = "Pattern1";
            } else if (kurtosis > 4 && kurtosis < 15 && spikes > 15 && pearson < 0.3 && maxYaw < 30
                    && avgYaw > 0.5 && avgPitch > 0.5 && maxPitch < 5) {
                suspicious = spikes > 16;
                patternType = "Pattern2";
            } else if (kurtosis > 20 && kurtosisHistory.stream().anyMatch(k -> k > 15) && spikes > 90 && pearson < 0.475) {
                suspicious = kurtosisHistory.stream().filter(k -> k > 15).count() > 2;
                patternType = "Pattern3";
            }
        }
        if (suspicious) {
            double bufferIncrement = (combatStarts < 3 || movementAfterStillness > 0) ? 0.3 : 0.5;
            double bufferThreshold = combatStarts < 3 ? 1.7 : 1.5;
            buffer = modifyBuffer(buffer, bufferIncrement);
            if (time() - lastFlag3 < 1000L) {
                return;
            }
            if (buffer >= bufferThreshold) {
                if (flagAndAlert("(Mix)\nt= " + patternType
                        + "\ndy= " + distinctYaw
                        + "\nks= " + String.format("%.2f", kurtosis)
                        + "\np= " + String.format("%.2f", pearson)
                        + "\nsp= " + spikes
                        + "\nstd= " + String.format("%.2f", stillnessDuration)
                        + "\nma= " + String.format("%.2f", movementAfterStillness))) {
                    mitigateDamage();
                    buffer = decayBuffer(buffer, 0.7, 0.4);
                }
            }
            lastFlag3 = time();
        } else {
            buffer = decayBuffer(buffer, 0.9, 0.5);
        }
        if (kurtosisHistory.size() >= 5) {
            kurtosisHistory.remove(0);
        }
        kurtosisHistory.add(kurtosis);
    }

    private void checkAdvancedPatterns(List<Float> yaws, double avgYaw) {
        double k4 = calculateFourthKurtosis(yaws);
        List<Double> outliers = detectOutliers(yaws);
        if (k4 > 14 && avgYaw > 0.75 && outliers.size() > 8) {
            if (time() - lastFlag4 < 1200L) {
                return;
            }
            buffer2 = modifyBuffer(buffer2, 1.2);
            if (buffer2 > 3.5) {
                if (flagAndAlert("(Adv)\nk4= " + String.format("%.2f", k4) + "\no= " + outliers.size())) {
                    mitigateDamage();
                    buffer2 = decayBuffer(buffer2, 0.85, 1.0);
                }
            }
            lastFlag4 = time();
        } else {
            buffer2 = decayBuffer(buffer2, 0.9, 1.5);
        }
    }

    private double maxAbs(List<Float> values) {
        double max = 0.0;
        for (float v : values) {
            max = Math.max(max, Math.abs(v));
        }
        return max;
    }

    private double calculateStillnessDuration(RotationUpdate update) {
        if (update.getDelta().getX() == 0 && update.getDelta().getY() == 0) {
            if (lastCombatTime == 0) {
                lastCombatTime = time();
            }
            return (time() - lastCombatTime) / 50.0;
        }
        lastCombatEndTime = time();
        lastCombatTime = 0;
        return 0.0;
    }

    private double calculateMovementAfterStillness() {
        if (rotations == null || rotations.isEmpty()) {
            return 0.0;
        }
        Vec2f last = rotations.get(rotations.size() - 1);
        return Math.abs(last.getX()) + Math.abs(last.getY());
    }

    private double calculateFourthKurtosis(List<Float> samples) {
        if (samples.size() < 10) {
            return 0.0;
        }
        double mean = MathUtil.getAverage(samples);
        double std = stdDev(samples);
        if (std < 0.01) {
            return 0.0;
        }
        double n = samples.size();
        double sum = 0.0;
        for (float value : samples) {
            double diff = (value - mean) / std;
            sum += Math.pow(diff, 4);
        }
        double kurtosis = (n * (n + 1) * sum) / ((n - 1) * (n - 2) * (n - 3));
        kurtosis -= (3 * Math.pow(n - 1, 2)) / ((n - 2) * (n - 3));
        return kurtosis;
    }

    private double kurtosis(List<Float> values) {
        List<Double> list = new ArrayList<>(values.size());
        for (float v : values) {
            list.add((double) v);
        }
        return MathUtil.getKurtosis(list, true);
    }

    private int distinct(List<Float> values) {
        Set<Float> set = new HashSet<>(values);
        return set.size();
    }

    private List<Double> detectOutliers(List<Float> values) {
        List<Double> list = new ArrayList<>(values.size());
        for (float v : values) {
            list.add((double) v);
        }
        return AnalysisMathUtil.zScoreOutliers(list, 1.0);
    }

    private double variation() {
        if (rotations == null || rotations.isEmpty()) {
            return 1.0;
        }
        List<Float> list = new ArrayList<>(rotations.size());
        for (Vec2f vec : rotations) {
            list.add(vec.getX());
        }
        double std = stdDev(list);
        double mean = MathUtil.getAverage(list);
        return mean == 0.0 ? 0.0 : std / mean;
    }

    private double consistency() {
        if (rotations == null || rotations.isEmpty()) {
            return 0.0;
        }
        int same = 0;
        for (int i = 1; i < rotations.size(); i++) {
            if (Math.signum(rotations.get(i).getX()) == Math.signum(rotations.get(i - 1).getX())) {
                same++;
            }
        }
        return rotations.size() <= 1 ? 0.0 : same / (double) (rotations.size() - 1);
    }

    private double stdDev(List<Float> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double mean = MathUtil.getAverage(values);
        double sum = 0.0;
        for (float v : values) {
            double diff = v - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / values.size());
    }

    private double modifyBuffer(double value, double inc) {
        return Math.max(0.0, value + inc);
    }

    private double decayBuffer(double value, double factor, double minCutoff) {
        value *= factor;
        return value < minCutoff ? 0.0 : value;
    }
}
