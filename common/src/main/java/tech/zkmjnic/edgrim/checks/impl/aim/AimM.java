package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;


@CheckData(
        name = "AimM",
        description = "Detecting linear rotation"
)
public class AimM extends Check implements RotationCheck {

    private double buffer;

    public AimM(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        float lastDeltaYaw = rotationUpdate.getProcessor().getLastDeltaYaw();
        float lastDeltaPitch = rotationUpdate.getProcessor().getLastDeltaPitch();
        float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        float diff = Math.abs(lastDeltaPitch / lastDeltaYaw - deltaPitch / deltaYaw);
        if (diff <= 0.01 && deltaYaw > 10 && deltaPitch > 10 && lastDeltaYaw > 10 && lastDeltaPitch > 10) {
            buffer++;
            if (buffer >= 5 && flagAndAlert("diff=" + diff + "\nbuf=" + buffer)) {
                player.mitigateDamage();
            }
        } else {
            buffer = 0;
        }
    }
}
