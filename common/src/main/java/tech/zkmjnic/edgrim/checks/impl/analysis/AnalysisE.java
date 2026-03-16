package tech.zkmjnic.edgrim.checks.impl.analysis;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.ExemptType;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.Pair2;
import tech.zkmjnic.edgrim.utils.lists.Tuple;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CheckData(name = "AnalysisE", configName = "AnalysisE", decay = 0.85, description = "Spike and entropy rotation analysis")
public final class AnalysisE extends AnalysisCheck implements RotationCheck {
    private final List<Vec2f> rawRotations;
    private final List<Pair2<Integer, Integer>> rotations2;
    private final List<Pair2<Double, Double>> kireikoGeneric;
    private long lastSpikeTime = 0;
    private int smallSpikeCount = 0;
    private List<Double> historySpikes;
    private double oldShannonYaw;
    private double oldShannonPitch;
    private int stabilityCheckCounter = 0;
    private double bufferStability;
    private double bufferVariation;
    private double bufferRatio;
    private double bufferChange;
    private double bufferLowCombo;
    private double bufferDistinct;
    private double bufferSpikes;
    private double bufferSpikesStd;
    private double bufferSpikesRatio;
    private double bufferSpikesRule;
    private double bufferSpikesRapid;
    private double bufferSpikesMega;
    private double bufferSpikesVariance;
    private double bufferSpikesOutlier;
    private double bufferSpikesDenseSmall;
    private double bufferSpikesStability;
    private double bufferMicroSpikes;

    public AnalysisE(EdGrimPlayer player) {
        super(player);
        this.rawRotations = new ArrayList<>();
        this.rotations2 = new ArrayList<>();
        this.kireikoGeneric = new ArrayList<>();
        this.historySpikes = new ArrayList<>(5);
        this.oldShannonYaw = 0;
        this.oldShannonPitch = 0;
    }

    @Override
    public void process(RotationUpdate update) {
        if (!isInCombat() || hasExemptions() || !shouldModifyPackets()) {
            decayAllBuffers(0.95, 0.3);
            rawRotations.clear();
            rotations2.clear();
            return;
        }

        if (player.getTarget() == null) {
            return;
        }

        Vec2f delta = update.getDelta();
        rawRotations.add(delta);

        double gcdValue = MathUtil.getGCDValueStatistic(0.5d) * 3;
        rotations2.add(new Pair2<>((int) (delta.getX() / gcdValue), (int) (delta.getY() / gcdValue)));

        if (rotations2.size() >= 10) {
            checkSpikes();
        }
        if (rawRotations.size() >= 10) {
            checkRaw(update);
        }
    }

    private void checkRaw(RotationUpdate update) {
        if (update.isCinematic()) {
            rawRotations.clear();
            return;
        }

        List<Float> xList = new ArrayList<>();
        List<Float> yList = new ArrayList<>();
        for (Vec2f vec : rawRotations) {
            xList.add(vec.getX());
            yList.add(vec.getY());
        }

        int sens = player.calculateSensitivity();
        AimProcessor processor = update.getProcessor();
        if (processor == null) {
            return;
        }
        int sensTemp = processor.totalSensitivityClient;
        double avgYaw = MathUtil.getAverage(xList);
        int distinctX = MathUtil.getDistinct(xList);
        double shannonYaw = AnalysisMathUtil.shannonEntropy(xList);
        double shannonPitch = AnalysisMathUtil.shannonEntropy(yList);
        boolean invalidSensitivity = sens < 50;
        boolean validSensitivity = sens >= 60 && sens <= 150 && sensTemp >= 60 && sensTemp < 150;
        double deltaYaw = Math.abs(shannonYaw - oldShannonYaw);
        double deltaPitch = Math.abs(shannonPitch - oldShannonPitch);

        if (validSensitivity && deltaYaw < 0.00015 && deltaPitch < 0.00015) {
            bufferStability = modifyBuffer(bufferStability, (shannonYaw > 3.0 && shannonPitch > 2.8) ? 0.8 : 1.2);
            if (bufferStability > 25.0) {
                if (flagAndAlert("(Stability)\nsy= " + String.format("%.3f", shannonYaw) + "\npd= " + String.format("%.5f", Math.abs(shannonPitch - oldShannonPitch)) + "\nyd= " + String.format("%.5f", Math.abs(shannonYaw - oldShannonYaw)))) {
                    bufferStability = decayBuffer(bufferStability, 0.84, 0.25);
                    mitigateDamage();
                }
            } else {
                bufferStability *= 0.98;
            }
        }

        if (deltaYaw < 0.005 && shannonPitch < 0.1 && player.onGround) {
            bufferChange = modifyBuffer(bufferChange, player.isSprinting ? 1.3 : 0.9);
            if (bufferChange > 6) {
                if (flagAndAlert("(Change)\nsy= " + String.format("%.3f", shannonYaw))) {
                    bufferChange = decayBuffer(bufferChange, 0.85, 0.35);
                    mitigateDamage();
                }
            }
        }

        if (!invalidSensitivity) {
            if (shannonYaw > 3.1 && shannonPitch < 2.0 && player.getPitch() < 37.5 && avgYaw > 0.085) {
                bufferLowCombo = modifyBuffer(bufferLowCombo, 0.5);
                if (bufferLowCombo > 5.0) {
                    if (flagAndAlert(String.format("(LowCombo)\nsy= %.3f\nsp= %.3f", shannonYaw, shannonPitch) + "\nv= " + validSensitivity)) {
                        mitigateDamage();
                        bufferLowCombo = decayBuffer(bufferLowCombo, 0.85, 0.25);
                    }
                } else {
                    bufferLowCombo = decayBuffer(bufferLowCombo, 1.0, 0.15);
                }
            }

            double entropyRatio = shannonYaw / (shannonPitch + 1e-5);
            boolean isLowPitchSpike = shannonPitch < 1.8 && shannonYaw > 2.9;
            if ((entropyRatio > 1.7 || entropyRatio < 0.45) && !isLowPitchSpike) {
                double sensFactor = 1.0 + (100 - Math.min(sens, 100)) * 0.015;
                bufferRatio = modifyBuffer(bufferRatio, 1.1 * sensFactor);
                if (bufferRatio > 3.0) {
                    if (flagAndAlert(String.format("(Ratio)\nr= %.2f\nsy= %.3f\nsp= %.3f\nsens= %d", entropyRatio, shannonYaw, shannonPitch, sens))) {
                        bufferRatio = modifyBuffer(bufferRatio, -0.5);
                        mitigateDamage();
                    }
                } else {
                    bufferRatio = decayBuffer(bufferRatio, 0.95, 0.15);
                }
            }

            if ((shannonYaw - oldShannonYaw) * (shannonPitch - oldShannonPitch) < -0.18 && Math.abs(shannonYaw - oldShannonYaw) > 0.25 && Math.abs(shannonPitch - oldShannonPitch) > 0.25) {
                bufferVariation = modifyBuffer(bufferVariation, (shannonYaw > 2.9 || shannonPitch > 2.8) ? 1.4 : 1.0);
                if (bufferVariation > 6.5) {
                    if (flagAndAlert(String.format("(Backward)\ny= %.3f\np= %.3f\nsy= %.3f\nsp= %.3f", deltaYaw, deltaPitch, shannonYaw, shannonPitch))) {
                        bufferVariation *= 0.7;
                        mitigateDamage();
                    }
                }
            }
        }

        if (distinctX < 8 && Math.abs(MathUtil.getAverage(xList)) > 2.5) {
            bufferDistinct = modifyBuffer(bufferDistinct, 1.7);
            if (bufferDistinct >= 4.0) {
                if (flagAndAlert("(Distinct)\ndistinct= " + distinctX + "\navg=" + MathUtil.getAverage(xList))) {
                    if (bufferDistinct >= 6) {
                        mitigateDamage();
                    }
                    bufferDistinct = modifyBuffer(bufferDistinct, -0.35);
                }
            }
        } else {
            bufferDistinct = modifyBuffer(bufferDistinct, -0.08);
        }

        oldShannonYaw = shannonYaw;
        oldShannonPitch = shannonPitch;
        rawRotations.clear();
    }

    private void checkSpikes() {
        List<Integer> gcdYaw = new ArrayList<>();
        List<Integer> gcdPitch = new ArrayList<>();
        for (Pair2<Integer, Integer> vec : rotations2) {
            gcdYaw.add(vec.getX());
            gcdPitch.add(vec.getY());
        }
        rotations2.clear();
        if (gcdYaw.isEmpty()) {
            return;
        }

        Tuple<List<Double>, List<Double>> outliers = AnalysisMathUtil.analyzeOutliers(gcdYaw);
        List<Double> yawOutX = outliers.getX();
        List<Double> yawOutY = outliers.getY();

        double currentMax = yawOutY.isEmpty() ? 0.0 : Math.max(Math.abs(MathUtil.getMax(yawOutY)), Math.abs(MathUtil.getMin(yawOutY)));
        double currentMin = yawOutY.isEmpty() ? 0.0 : Math.min(Math.abs(MathUtil.getMax(yawOutY)), Math.abs(MathUtil.getMin(yawOutY)));

        Pair2<Double, Double> kireikoVec = new Pair2<>(AnalysisMathUtil.kireikoGeneric(gcdYaw), AnalysisMathUtil.kireikoGeneric(gcdPitch));
        kireikoGeneric.add(kireikoVec);
        if (kireikoGeneric.size() >= 7) {
            List<Double> kireikoX = new ArrayList<>();
            List<Double> kireikoY = new ArrayList<>();
            for (Pair2<Double, Double> vec : kireikoGeneric) {
                kireikoX.add(vec.getX());
                kireikoY.add(vec.getY());
            }

            double stdDevX = MathUtil.stdDev(kireikoX);
            Tuple<Double, Double> spikeRange = new Tuple<>(MathUtil.getMin(kireikoX), MathUtil.getMax(kireikoX));

            if (stdDevX > 10 && stdDevX < 22 && spikeRange.getY() < 50) {
                bufferSpikesRule = modifyBuffer(bufferSpikesRule, MathUtil.getAverage(kireikoX) < 6.0 ? 0 : (stdDevX < 10 ? 1.5 : 1.0));
                if (bufferSpikesRule > 7.0) {
                    if (flagAndAlert("(Spike Rule) \nstdDevX=" + (int) stdDevX)) {
                        mitigateDamage();
                        EdGrimAPI.INSTANCE.getScheduler().getAsyncScheduler()
                                .runDelayed(EdGrimAPI.INSTANCE.getGrimPlugin(), this::mitigateDamage, 1000L, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        if (historySpikes.size() >= 15) {
            historySpikes = new ArrayList<>(historySpikes.subList(Math.max(0, historySpikes.size() - 14), historySpikes.size()));
        }
        historySpikes.add(currentMax);

        if (currentMax > 35 && currentMax < 65) {
            smallSpikeCount = Math.min(smallSpikeCount + 1, 5);
        } else {
            smallSpikeCount = Math.max(0, smallSpikeCount - 2);
        }
        if (smallSpikeCount >= 3) {
            bufferSpikesDenseSmall = modifyBuffer(bufferSpikesDenseSmall, 1.3);
            if (bufferSpikesDenseSmall > 5.2) {
                if (flagAndAlert("(Dense Small)\ncount= " + smallSpikeCount)) {
                    mitigateDamage();
                    bufferSpikesDenseSmall = decayBuffer(bufferSpikesDenseSmall, 0.75, 0.25);
                }
            } else {
                bufferSpikesDenseSmall = decayBuffer(bufferSpikesDenseSmall, 0.95, 0.25);
            }
            smallSpikeCount = 0;
        }

        if (gcdYaw.size() > 8) {
            List<Double> last8 = gcdYaw.stream().skip(gcdYaw.size() - 8).map(Integer::doubleValue).collect(Collectors.toList());
            double microEntropy = AnalysisMathUtil.microChangeEntropy(last8);
            double avgChange = MathUtil.getAverage(last8);

            if (microEntropy < 0.8 && avgChange > 28 && currentMax < 45 && MathUtil.getMax(last8) - MathUtil.getMin(last8) > 60) {
                bufferMicroSpikes = modifyBuffer(bufferMicroSpikes, 1.8);
                if (bufferMicroSpikes > 3.5) {
                    flagAndAlert("(Micro Spike)\nentropy=" + String.format("%.2f", microEntropy));
                }
            } else {
                bufferMicroSpikes = decayBuffer(bufferMicroSpikes, 0.9, 0.2);
            }
        }

        if (gcdYaw.size() > 15 && stabilityCheckCounter++ % 3 == 0) {
            double stability = AnalysisMathUtil.stabilityIndex(gcdYaw);
            if (stability < 0.4 && currentMax > 45) {
                bufferSpikesStability = modifyBuffer(bufferSpikesStability, 1.6);
                if (bufferSpikesStability > 3.8) {
                    if (flagAndAlert("(Stability)\nstability= " + String.format("%.2f", stability))) {
                        mitigateDamage();
                        bufferSpikesStability = decayBuffer(bufferSpikesStability, 0.75, 0.25);
                    }
                } else {
                    bufferSpikesStability = decayBuffer(bufferSpikesStability, 0.95, 0.25);
                }
            }
        }

        double varianceX = MathUtil.getVariance(gcdYaw);
        double varianceY = MathUtil.getVariance(gcdPitch);
        double minVar = Math.min(varianceX, varianceY);
        double maxVar = Math.max(varianceX, varianceY);
        if (minVar < 0.09 && maxVar > 35 && MathUtil.getMin(gcdPitch) != 0.0) {
            bufferSpikesVariance = modifyBuffer(bufferSpikesVariance, 1.0);
            if (bufferSpikesVariance > 2.5) {
                if (flagAndAlert("(Variance)\nminVar= " + minVar + "\nmaxVar= " + maxVar + "\nminGcdPitch= " + MathUtil.getMin(gcdPitch))) {
                    rewardBufferAndVL();
                    mitigateDamage();
                }
            }
        } else {
            bufferSpikesVariance = modifyBuffer(bufferSpikesVariance, -0.4);
        }

        if (!yawOutY.isEmpty() && yawOutY.size() < 4) {
            AimProcessor processor = player.checkManager.getRotationCheck(AimProcessor.class);
            if (processor != null && processor.getDeltaYaw() < 25) {
                if (currentMax > 150) {
                    bufferSpikesMega = modifyBuffer(bufferSpikesMega, 2.0);
                    if (bufferSpikesMega > 5.5) {
                        if (flagAndAlert("(Mega Spike)\nval= " + currentMax)) {
                            mitigateDamage();
                            bufferSpikesMega = decayBuffer(bufferSpikesMega, 0.75, 0.25);
                        }
                    } else {
                        bufferSpikesMega = decayBuffer(bufferSpikesMega, 0.95, 0.15);
                    }
                }

                if (currentMax > 60 && currentMax <= 150) {
                    if (lastSpikeTime != 0 && time() - lastSpikeTime < 1200) {
                        bufferSpikesRapid = modifyBuffer(bufferSpikesRapid, 1.8);
                        if (bufferSpikesRapid > 3.5) {
                            if (flagAndAlert("(Rapid Spikes)\ncount= " + yawOutY.size())) {
                                mitigateDamage();
                                bufferSpikesRapid = decayBuffer(bufferSpikesRapid, 0.85, 0.25);
                            }
                        } else {
                            bufferSpikesRapid = decayBuffer(bufferSpikesRapid, 0.95, 0.15);
                        }
                        lastSpikeTime = time();
                    }

                    if (currentMin > 10 && currentMax / currentMin > 3.5) {
                        bufferSpikesRatio = modifyBuffer(bufferSpikesRatio, 1.2);
                        if (bufferSpikesRatio > 4.2) {
                            if (flagAndAlert("(Ratio Spike)\nmax/min= " + (currentMax / currentMin))) {
                                mitigateDamage();
                                bufferSpikesRatio = decayBuffer(bufferSpikesRatio, 0.85, 0.25);
                            }
                        } else {
                            bufferSpikesRatio = decayBuffer(bufferSpikesRatio, 0.95, 0.15);
                        }
                    }
                }

                if (gcdYaw.size() > 10) {
                    double rollingStd = AnalysisMathUtil.rollingStdDev(gcdYaw, 5);
                    if (rollingStd > 45 && MathUtil.getAverage(gcdYaw) < 30) {
                        bufferSpikesStd = modifyBuffer(bufferSpikesStd, 1.5);
                        if (bufferSpikesStd > 3.0) {
                            if (flagAndAlert("(Std Surge)\nstd= " + rollingStd)) {
                                mitigateDamage();
                                bufferSpikesStd = decayBuffer(bufferSpikesStd, 0.85, 0.25);
                            }
                        } else {
                            bufferSpikesStd = decayBuffer(bufferSpikesStd, 0.95, 0.15);
                        }
                    }
                }

                if ((currentMax > 55 && currentMin == currentMax) || (currentMax > 60 && currentMin < currentMax / 3)) {
                    bufferSpikesOutlier = modifyBuffer(bufferSpikesOutlier, 1.45);
                    if (bufferSpikesOutlier > 4.0) {
                        if (flagAndAlert("(Spike Rough) \nOutX= " + yawOutX)) {
                            bufferSpikesOutlier = modifyBuffer(bufferSpikesOutlier, -0.45);
                        }
                    }
                }
            }
        } else {
            bufferSpikesOutlier = modifyBuffer(bufferSpikesOutlier, -0.35);
        }
    }

    private boolean isInCombat() {
        return hasAttackedSince(800L);
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

    private void decayAllBuffers(double factor, double threshold) {
        bufferSpikesOutlier = decayBuffer(bufferSpikesOutlier, factor, threshold);
        bufferStability = decayBuffer(bufferStability, factor, threshold);
        bufferVariation = decayBuffer(bufferVariation, factor, threshold);
        bufferRatio = decayBuffer(bufferRatio, factor, threshold);
        bufferChange = decayBuffer(bufferChange, factor, threshold);
        bufferLowCombo = decayBuffer(bufferLowCombo, factor, threshold);
        bufferDistinct = decayBuffer(bufferDistinct, factor, threshold);
        bufferSpikes = decayBuffer(bufferSpikes, factor, threshold);
        bufferSpikesStd = decayBuffer(bufferSpikesStd, factor, threshold);
        bufferSpikesRatio = decayBuffer(bufferSpikesRatio, factor, threshold);
        bufferSpikesRule = decayBuffer(bufferSpikesRule, factor, threshold);
        bufferSpikesRapid = decayBuffer(bufferSpikesRapid, factor, threshold);
        bufferSpikesMega = decayBuffer(bufferSpikesMega, factor, threshold);
        bufferSpikesVariance = decayBuffer(bufferSpikesVariance, factor, threshold);
        bufferSpikesOutlier = decayBuffer(bufferSpikesOutlier, factor, threshold);
        bufferMicroSpikes = decayBuffer(bufferMicroSpikes, factor, threshold);
        reward();
    }

    private double decayBuffer(double buffer, double factor, double threshold) {
        buffer *= factor;
        if (buffer < threshold) {
            buffer = 0;
        }
        return buffer;
    }

    private double modifyBuffer(double buffer, double amount) {
        buffer += amount;
        return Math.max(buffer, 0);
    }
}
