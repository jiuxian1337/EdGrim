package tech.zkmjnic.edgrim.checks.impl.aim;

import lombok.Setter;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import tech.zkmjnic.edgrim.utils.time.Watch;

import java.util.*;

// Original author: Dg32z
// https://github.com/Dg32z
@CheckData(name = "AimD", configName = "AimD", decay = 0.85, experimental = true)
public final class AimD extends Check implements RotationCheck {

    private static final long COMBAT_COOLDOWN_MS = 100;
    private static final int ANALYSIS_MIN_SAMPLES = 60;
    private static final long ANALYSIS_INTERVAL_MS = 275;
    private final Watch timer = new Watch();
    private final List<Double> kurtosisHistory = new ArrayList<>();
    private final List<Float> yawEntropyHist = new ArrayList<>(), pitchEntropyHist = new ArrayList<>();
    private final List<Integer> dYHistory = new ArrayList<>(10);
    List<Vec2f> rotation = new ArrayList<>();
    int counter = 0;
    private long lastFlag;
    private long lastFlag2;
    private long lastFlag3;
    private long lastFlag4;
    private double buffer;
    private double buffer2;
    private double buffer3;
    private double buffer4;
    private double buffer5;
    @Setter
    private List<Vec2f> rotations;
    private long lastCombatEndTime = 0;
    private long lastCombatTime = 0;

    public AimD(PlayerData player) {
        super(player);
    }


    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!player.actionManager.hasAttackedSince(1400L)) {
            rotation.clear();
            setRotations(null);
            return;
        }

        if (Math.abs(rotationUpdate.getTo().getPitch()) >= 89.9F) return;

        if (player.packetStateData.lastPacketWasTeleport
                || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
            return;
        }

        final Vec2f delta = rotationUpdate.getDelta();
        final float deltaYawAbs = Math.abs(delta.getX());
        final float deltaPitchAbs = Math.abs(delta.getY());

        // Ignore very small housekeeping rotations so meaningful combat input reaches
        // analysis thresholds faster instead of being drowned in tiny noise.
        if (deltaYawAbs < 0.35f && deltaPitchAbs < 0.2f) {
            return;
        }

        rotation.add(delta);

        if (player.calculateSensitivity() < 40) {
            rotation.clear();
            setRotations(null);
            return;
        }

        if (rotation.size() >= ANALYSIS_MIN_SAMPLES) {
            setRotations(rotation);
            checkForAimbot(rotationUpdate.getProcessor(), rotationUpdate);
        }
    }


    public void checkForAimbot(AimProcessor processor, RotationUpdate update) {
        if (rotations == null) {
            return;
        }

        if (rotations.size() < ANALYSIS_MIN_SAMPLES
                || !timer.hasTimeElapsed(ANALYSIS_INTERVAL_MS)
        ) return;
        timer.reset();

        List<Float> yaws = new ArrayList<>(rotations.size());
        List<Float> pitches = new ArrayList<>(rotations.size());

        for (Vec2f rot : rotations) {
            yaws.add(rot.getX());
            pitches.add(rot.getY());
        }

        float avgYaw = processor.getAvgYaw();
        float avgPitch = processor.getAvgPitch();
        float maxYaw = processor.getMaxYaw();
        float maxPitch = processor.getMaxPitch();
        double sens = player.calculateSensitivity();

        if (sens < 35) {
            rotations.clear();
            return;
        }

        checkBasicPatterns(avgYaw);

        if (rotations.size() > 50) {
            checkDKS(update, yaws, pitches, avgYaw, maxYaw, avgPitch, maxPitch);
            if (update.isCinematic2()) {
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
        double sum = 0;
        long count = 0;
        for (Float v : hist) {
            double abs = Math.abs(v - current);
            sum += abs;
            count++;
        }
        return count > 0 ? sum / count : 0;
    }


    private void checkEntropyPatterns(List<Float> yaws, List<Float> pitches, float avgYaw, float avgPitch) {
        double yawEnt = MathUtil.calculateNEntropy(yaws), pitchEnt = MathUtil.calculateNEntropy(pitches);
        double similarScore = Math.abs(yawEnt - pitchEnt);

        yawEntropyHist.add((float) yawEnt);
        pitchEntropyHist.add((float) pitchEnt);
        int ENTROPY_HISTORY = 7;
        if (yawEntropyHist.size() > ENTROPY_HISTORY) {
            yawEntropyHist.remove(0);
            pitchEntropyHist.remove(0);
        }

        if (yawEntropyHist.size() >= ENTROPY_HISTORY) {
            double yawVar = avgVar(yawEntropyHist, yawEnt);
            double pitchVar = avgVar(pitchEntropyHist, pitchEnt);

            boolean isSuspicious = false;
            String reason = "";

            if (similarScore > 3) {
                isSuspicious = true;
                reason = String.format("[H] ss= %.3f", similarScore);
            } else if (similarScore < 0.01 && similarScore != 0 && avgPitch > 1 && avgYaw > 1) {
                isSuspicious = true;
                reason = String.format("[L] ss= %.3f", similarScore);
            } else if (((yawEnt < 1.0 && pitchEnt > 2.0) || (pitchEnt < 1.0 && yawEnt > 2.0)) && avgYaw > 2 && avgPitch > 2 && (yawEnt >= 0.01 && pitchEnt >= 0.01)) {
                isSuspicious = true;
                reason = String.format("[LOS] y= %.3f\np= %.3f", yawEnt, pitchEnt);
            } else if (yawEnt < 0.5 && pitchEnt < 0.5 && yawVar < 0.1 && pitchVar < 0.1 && avgYaw > 2 && avgPitch > 2 && (yawEnt >= 0.01 && pitchEnt >= 0.01)) {
                isSuspicious = true;
                reason = String.format("[BLS] y=%.3f\np= %.3f", yawEnt, pitchEnt);
            }

            if (isSuspicious) {
                buffer5 = modifyBuffer(buffer5, 1.0);
                if (buffer5 > 3) {
                    if (flagAndAlert("(Entropy)\nMain= " + reason + "\ny= " + yawEnt + "\npe= " + pitchEnt + "\nyv= " + yawVar + "\npv= " + pitchVar)) {
                        player.mitigateDamage();
                        buffer5 = decayBuffer(buffer5, 0.95, 1.0);
                    }
                }
            } else {
                buffer5 = decayBuffer(buffer5, 0.85, 1.5);
            }
        }
    }

    private void checkBasicPatterns(float avgYaw) {
        double var = variation(), cons = consistency();

        if (var < 0.08 && avgYaw > 1) {
            if (System.currentTimeMillis() - lastFlag < 800L) {
                return;
            }
            buffer3 = modifyBuffer(buffer3, 1.0);
            if (buffer3 > 4) {
                if (flagAndAlert("(Basic)\nvar= " + var)) {
                    player.mitigateDamage();
                    buffer3 = decayBuffer(buffer3, 0.75, 1.0);
                }
            }
            lastFlag = System.currentTimeMillis();
        } else {
            buffer3 = decayBuffer(buffer3, 0.85, 2.0);
        }
        if (cons > 0.85) {
            if (System.currentTimeMillis() - lastFlag2 < 500L) {
                return;
            }
            buffer4 = modifyBuffer(buffer4, 1.0);
            if (buffer4 > 4) {
                if (flagAndAlert("(Basic)\nc= " + cons)) {
                    player.mitigateDamage();
                    buffer4 = decayBuffer(buffer4, 0.75, 1.5);
                }
            }
            lastFlag2 = System.currentTimeMillis();
        } else {
            buffer4 = decayBuffer(buffer4, 0.95, 2.0);
        }
    }

    private double calculateFourthKurtosis(List<Float> samples) {
        if (samples.size() < 10) return 0; // Need sufficient samples

        double mean = MathUtil.getAverage(samples);
        double stdDev = stdDev(samples);
        if (stdDev < 0.01) return 0; // Ignore near-zero deviation

        double n = samples.size();
        double sum = 0;

        // Calculate fourth moment
        for (float value : samples) {
            double diff = (value - mean) / stdDev;
            sum += Math.pow(diff, 4);
        }

        // Corrected kurtosis calculation with bias adjustment
        double kurtosis = (n * (n + 1) * sum) / ((n - 1) * (n - 2) * (n - 3));
        kurtosis -= (3 * Math.pow(n - 1, 2)) / ((n - 2) * (n - 3));

        return kurtosis;
    }


    private boolean isLegitPattern(int dY, double kurtosis, double pearson, int spikes) {
        if (dY > 30 && dY < 80 &&
                Math.abs(kurtosis) < 3.0 &&
                pearson > 0.1 && pearson < 0.7) {
            return true;
        }

        if (kurtosis > 15 && kurtosisHistory.stream().noneMatch(k -> k > 10) &&
                dY > 20 && spikes < 15) {
            return true;
        }

        if (!dYHistory.isEmpty()) {
            double growthRate = (double) dY / dYHistory.get(dYHistory.size() - 1);
            return growthRate > 1.2 && growthRate < 2.5 &&
                    dY > 15 && kurtosis < 5.0;
        }

        return false;
    }


    // Modified detection logic
    private void checkDKS(RotationUpdate update, List<Float> yaws, List<Float> pitches, float avgYaw, float maxYaw, float avgPitch, float maxPitch) {
        // Skip detection during combat cooldown
        if (System.currentTimeMillis() - lastCombatEndTime < COMBAT_COOLDOWN_MS) {
            return;
        }

        int combatStarts = counter;

        int distinctYaw = distinct(yaws);
        int spikes = detectOutliers(yaws).size() + detectOutliers(pitches).size();
        double kurtosis = kurtosis(yaws), pearson = pearson(yaws, pitches);

        if (dYHistory.size() >= 10) {
            dYHistory.remove(0);
        }
        dYHistory.add(distinctYaw);

        double stillnessDuration = calculateStillnessDuration(update);
        double movementAfterStillness = calculateMovementAfterStillness();

        boolean isLegit = isLegitPattern(distinctYaw, kurtosis, pearson, spikes);

        boolean isSuspicious = false;
        String patternType = "Normal";
        if (!isLegit) {
            if (distinctYaw < 20 && kurtosis > 15 && pearson < 0.5 && avgYaw > 0.5) {
                isSuspicious = combatStarts > 1;
                patternType = "Pattern1";
            } else if (kurtosis > 4 && kurtosis < 15 && spikes > 15 && pearson < 0.3 && maxYaw < 30 && avgYaw > 0.5 && avgPitch > 0.5 && maxPitch < 5) {
                isSuspicious = spikes > 16;
                patternType = "Pattern2";
            } else if (kurtosis > 20 && kurtosisHistory.stream().anyMatch(k -> k > 15) && spikes > 90 && pearson < 0.475) {
                isSuspicious = kurtosisHistory.stream().filter(k -> k > 15).count() > 2;
                patternType = "Pattern3";
            }
        }

        if (isSuspicious) {
            double bufferIncrement = (combatStarts < 3 || movementAfterStillness > 0) ? 0.3 : 0.5;
            double bufferThreshold = combatStarts < 3 ? 1.7 : 1.5;

            buffer = modifyBuffer(buffer, bufferIncrement);

            if (System.currentTimeMillis() - lastFlag3 < 1000L) {
                return;
            }

            if (buffer >= bufferThreshold) {
                if (flagAndAlert("(Mix)\nt= " + patternType
                        + "\ndy= " + distinctYaw
                        + "\nk= " + kurtosis
                        + "\np= " + pearson
                        + "\ns= " + spikes
                        + "\nst= " + stillnessDuration
                        + "\nmas= " + movementAfterStillness
                        + "\nb= " + buffer)) {
                    player.mitigateDamage();
                }
                lastFlag3 = System.currentTimeMillis();
            }
        } else {
            buffer = decayBuffer(buffer, 0.2, 1.0);
        }

        if (player.actionManager.hasAttackedSince(1200L)) {
            counter++;
            lastCombatTime = System.currentTimeMillis();
        } else {
            lastCombatEndTime = System.currentTimeMillis();
            if (System.currentTimeMillis() - lastCombatTime > 5000) {
                counter = 0;
            }
        }

        kurtosisHistory.add(kurtosis);
        if (kurtosisHistory.size() > 10) kurtosisHistory.remove(0);
    }

    private double calculateStillnessDuration(RotationUpdate update) {
        long stillnessStart = -1;
        long currentTime = System.currentTimeMillis();

        for (int i = rotations.size() - 1; i >= 0; i--) {
            Vec2f rot = rotations.get(i);
            if (Math.abs(rot.getX()) > 0.1 || Math.abs(rot.getY()) > 0.1) {
                break;
            }
            stillnessStart = update.getTick();
        }

        return stillnessStart == -1 ? 0 : currentTime - stillnessStart;
    }

    private double calculateMovementAfterStillness() {
        if (rotations.size() < 2) return 0;

        for (int i = 1; i < rotations.size(); i++) {
            Vec2f prev = rotations.get(i - 1);
            Vec2f curr = rotations.get(i);

            if (Math.abs(prev.getX()) < 0.1 && Math.abs(prev.getY()) < 0.1) {
                if (Math.abs(curr.getX()) >= 0.1 || Math.abs(curr.getY()) >= 0.1) {
                    return Math.sqrt(
                            Math.pow(curr.getX() - prev.getX(), 2) +
                                    Math.pow(curr.getY() - prev.getY(), 2)
                    );
                }
            }
        }

        return 0;
    }


    private void checkAdvancedPatterns(List<Float> yaws, float avgYaw) {
        double kurtosis = kurtosis(yaws);


        if (Math.abs(kurtosis) > 3.0) {
            kurtosisHistory.add(kurtosis);
            if (kurtosisHistory.size() > 15) kurtosisHistory.remove(0);
        }


        kurtosisHistory.add(kurtosis);
        if (kurtosisHistory.size() > 8) kurtosisHistory.remove(0);

        double fKurtosis = calculateFourthKurtosis(yaws);
        double absKurtosis = Math.abs(fKurtosis);

        double kurtosisThreshold = 15 + (10 * Math.log10(yaws.size()));

        if (absKurtosis > kurtosisThreshold) {
            int similarCount = countSimilarKurtosis(fKurtosis);
            if (similarCount > 2 && avgYaw > 5) { // Require multiple consecutive detections
                if (System.currentTimeMillis() - lastFlag4 < 500L) {
                    return;
                }
                buffer2 = modifyBuffer(buffer2, 2);
                if (buffer2 > 8) {
                    if (flagAndAlert("(Basic)\nk= " + kurtosis)) {
                        player.mitigateDamage();
                    }
                }
                lastFlag4 = System.currentTimeMillis();
            }
        }

        if (absKurtosis > 1.0) {
            kurtosisHistory.add(fKurtosis);
            if (kurtosisHistory.size() > 12) kurtosisHistory.remove(0);
        }
    }

    private int countSimilarKurtosis(double currentKurtosis) {
        return (int) kurtosisHistory.stream().filter(k -> Math.abs(k - currentKurtosis) < 2.0).count();
    }

    private double modifyBuffer(double buffer, double amount) {
        return Math.max(0, buffer + amount);
    }

    private double decayBuffer(double buffer, double decay, double min) {
        return Math.max(min, buffer * decay);
    }

    private double variation() {
        double[] stats = rotations.stream()
                .filter(r -> Math.abs(r.getX()) >= 0.001 || Math.abs(r.getY()) >= 0.001)
                .collect(() -> new double[3], (a, r) -> {
                    a[0] += r.getX() + r.getY();
                    a[1] += Math.pow(r.getX(), 2) + Math.pow(r.getY(), 2);
                    a[2] += 2;
                }, (a, b) -> {
                    a[0] += b[0];
                    a[1] += b[1];
                    a[2] += b[2];
                });
        return stats[2] == 0 ? 0 : Math.sqrt((stats[1] / stats[2]) - Math.pow(stats[0] / stats[2], 2));
    }

    private double consistency() {
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        int bound = rotations.size();
        for (int i1 = 1; i1 < bound; i1++) {
            Vec2f prev = rotations.get(i1 - 1), curr = rotations.get(i1);
            double yaw = Math.round((curr.getX() - prev.getX()) * 10) / 10.0;
            double pitch = Math.round((curr.getY() - prev.getY()) * 10) / 10.0;
            int changes = 0;
            if (Math.abs(yaw) >= 0.1) {
                counts.merge(String.valueOf(yaw), 1, Integer::sum);
                changes++;
            }
            if (Math.abs(pitch) >= 0.1) {
                counts.merge(String.valueOf(pitch), 1, Integer::sum);
                changes++;
            }
            int applyAsInt = changes;
            total += applyAsInt;
        }
        if (total == 0) {
            return 0;
        } else {
            boolean seen = false;
            Integer best = null;
            for (Integer i : counts.values()) {
                if (!seen || i > best) {
                    seen = true;
                    best = i;
                }
            }
            return (double) (seen ? best : 0) / total;
        }
    }


    private List<Double> detectOutliers(List<Float> data) {
        double mean = MathUtil.getAverage(data), std = stdDev(data);
        List<Double> list = new ArrayList<>();
        for (Float v : data) {
            double z = (Math.abs(v) - mean) / std;
            if (Math.abs(z) > (float) 1.0) {
                list.add(z);
            }
        }
        return list;
    }

    private int distinct(List<Float> data) {
        long count = 0L;
        Set<String> uniqueValues = new HashSet<>();
        for (Float v : data) {
            String format = String.format("%.2f", v);
            if (uniqueValues.add(format)) {
                count++;
            }
        }
        return (int) count;
    }

    private double kurtosis(List<Float> data) {
        double mean = MathUtil.getAverage(data), std = stdDev(data);
        if (std == 0) return 0;
        double sum = 0;
        long count = 0;
        for (Float v : data) {
            double pow = Math.pow((v - mean) / std, 4);
            sum += pow;
            count++;
        }
        return (count > 0 ? sum / count : 0) - 3;
    }

    private double pearson(List<Float> x, List<Float> y) {
        if (x.size() != y.size() || x.size() < 2) return 0;
        double meanX = MathUtil.getAverage(x), meanY = MathUtil.getAverage(y), cov = 0, varX = 0, varY = 0;
        for (int i = 0; i < x.size(); i++) {
            double dx = Math.abs(x.get(i)) - meanX, dy = Math.abs(y.get(i)) - meanY;
            cov += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        return (varX == 0 || varY == 0) ? 0 : cov / (Math.sqrt(varX) * Math.sqrt(varY));
    }

    private double stdDev(List<Float> data) {
        double mean = MathUtil.getAverage(data);
        double sum = 0;
        long count = 0;
        for (Float v : data) {
            double pow = Math.pow(v - mean, 2);
            sum += pow;
            count++;
        }
        return Math.sqrt(count > 0 ? sum / count : 0);
    }
}
