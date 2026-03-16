package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.Tuple;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@CheckData(name = "AimL", configName = "AimL", decay = 0.75, description = "Detects high variance with stable averages in rotation samples")
public final class AimL extends EdAimCheck {
    private final Deque<Float> samplesYaw;
    private final Deque<Float> samplesPitch;

    private double lastAverageYaw;
    private double lastAveragePitch;

    private double maxBuffer;

    public AimL(EdGrimPlayer player) {
        super(player);
        lastAverageYaw = 0;
        lastAveragePitch = 0;
        samplesYaw = new LinkedList<>();
        samplesPitch = new LinkedList<>();
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();

        final boolean attacking = hasAttackedSince(800L);

        if (deltaYaw == 0.0F || deltaPitch == 0.0F || deltaYaw > 30F || deltaPitch > 30F || !isMoving() || !attacking) {
            return;
        }

        samplesYaw.add(deltaYaw);
        samplesPitch.add(deltaPitch);

        if (samplesYaw.size() >= 20 && samplesPitch.size() >= 20) {

            final double deviationYaw = MathUtil.getStandardDeviation(samplesYaw);
            final double deviationPitch = MathUtil.getStandardDeviation(samplesPitch);

            final double averageYaw = MathUtil.getAverage(samplesYaw);
            final double averagePitch = MathUtil.getAverage(samplesPitch);

            final double differenceYaw = Math.abs(averageYaw - lastAverageYaw);
            final double differencePitch = Math.abs(averagePitch - lastAveragePitch);

            Tuple<List<Double>, List<Double>>
                    outliersYaw = MathUtil.getOutliers(samplesYaw),
                    outliersPitch = MathUtil.getOutliers(samplesPitch);

            final int outlierCountYaw = outliersYaw.getX().size() + outliersYaw.getY().size();
            final int outlierCountPitch = outliersPitch.getX().size() + outliersPitch.getY().size();

            if (deviationYaw > 6.0 && deviationPitch > 6.0
                    && differenceYaw < 1.5 && differencePitch < 1.5
                    && outlierCountYaw < 15 && outlierCountPitch < 15) {
                buffer += 2.0;
                if (buffer > maxBuffer) {
                    if (flagAndAlert("devX= " + deviationYaw
                            + "\ndevY= " + deviationPitch
                            + "\nyDiff= " + differencePitch
                            + "\nxDiff= " + differenceYaw
                            + "\noX= " + outlierCountYaw
                            + "\noY= " + outlierCountPitch)) {
                        mitigateDamage();
                        buffer = 0;
                    }
                }
            } else {
                rewardBufferAndVL();
            }
            lastAverageYaw = averageYaw;
            lastAveragePitch = averagePitch;
            samplesYaw.clear();
            samplesPitch.clear();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getDoubleElse(getConfigName() + ".buffer", 4);
    }
}
