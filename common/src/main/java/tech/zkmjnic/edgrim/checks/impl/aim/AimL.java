package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.Tuple;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.List;

@CheckData(
        name = "AimL",
        configName = "AimL",
        decay = 0.86
)
public final class AimL extends Check implements RotationCheck {
    private final List<Float> samplesYaw = new ArrayList<>();
    private final List<Float> samplesPitch = new ArrayList<>();
    private double buffer2;

    public AimL(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        final boolean invalidSensitivity = player.calculateSensitivity() < 75 || player.calculateSensitivity() > 175;
        if (update.isCinematic2() || invalidSensitivity || !player.actionManager.hasAttackedSince(600L)) {
            return;
        }

        final float deltaYaw = update.getProcessor().getDeltaYaw();
        final float lastDeltaYaw = update.getProcessor().getLastDeltaYaw();
        final float deltaPitch = update.getProcessor().getDeltaPitch();
        final float lastDeltaPitch = update.getProcessor().getLastDeltaPitch();
        final float differenceYaw = Math.abs(deltaYaw - lastDeltaYaw);
        final float differencePitch = Math.abs(deltaPitch - lastDeltaPitch);
        final float joltX = Math.abs(deltaYaw - differenceYaw);
        final float joltY = Math.abs(deltaPitch - differencePitch);

        samplesYaw.add((float) MathUtil.roundToPlace(joltX, 2));
        samplesPitch.add((float) MathUtil.roundToPlace(joltY, 2));

        if (samplesYaw.size() + samplesPitch.size() >= 60) {
            if (!(joltX == 0.0 || joltY == 0.0)) {
                final Tuple<List<Double>, List<Double>> outliersYaw = MathUtil.getOutliers(samplesYaw);
                final Tuple<List<Double>, List<Double>> outliersPitch = MathUtil.getOutliers(samplesPitch);
                final int distinctYaw = (int) samplesYaw.stream().distinct().count();
                final int distinctPitch = (int) samplesPitch.stream().distinct().count();
                final int duplicatesX = samplesYaw.size() - distinctYaw;
                final int duplicatesY = samplesPitch.size() - distinctPitch;
                final int duplicatesSum = duplicatesX + duplicatesY;
                final int outliersX = outliersYaw.getX().size() + outliersYaw.getY().size();
                final int outliersY = outliersPitch.getX().size() + outliersPitch.getY().size();

                if (duplicatesSum <= 3 && outliersX < 10 && outliersY < 7) {
                    if (buffer++ > 4) {
                        if (flagAndAlert("d= " + duplicatesSum + "\nox= " + outliersX + "\noy= " + outliersY)) {
                            player.mitigateDamage();
                        }
                    } else {
                        rewardBufferAndVL();
                    }
                } else if ((outliersX == 0 || outliersY == 0) && (outliersX > 1 || outliersY > 1) && duplicatesSum <= 3) {
                    if (buffer2++ > 2 && flagAndAlert("d= " + duplicatesSum + "\nox= " + outliersX + "\noy= " + outliersY)) {
                        player.mitigateDamage();
                    }
                } else {
                    if (buffer2 == 0.0) {
                        rewardVL();
                    } else {
                        buffer2 = Math.max(0, buffer2 - getDecay());
                    }
                }
            }
            samplesYaw.clear();
            samplesPitch.clear();
        }
    }
}
