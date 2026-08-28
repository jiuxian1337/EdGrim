package cc.watchneko.checks.impl.aim;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;


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
