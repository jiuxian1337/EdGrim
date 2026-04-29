package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.util.AimTargetTraceUtil;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;

@CheckData(
        name = "AimN",
        description = "consistent target-angle lock"
)
public final class AimN extends Check implements RotationCheck {
    private double lastAngle;
    private double lastAngleDiff;

    public AimN(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!player.actionManager.hasAttackedSince(2000L)) {
            buffer = 0.0;
            return;
        }

        PacketEntity target = AimTargetTraceUtil.getPlayerTarget(player);
        if (target == null || target.type != EntityTypes.PLAYER) {
            return;
        }

        if (player.actionManager.hasAttackedSince(1000L) || player.getDeltaXZ() <= 0.1) {
            return;
        }

        float deltaPitch = update.getDeltaXRotABS();
        float deltaYaw = update.getDeltaYRotABS();
        double angle = AimTargetTraceUtil.angleToCenter(player, update.getTo().getYaw(), AimTargetTraceUtil.getTargetBox(target));
        double angleDiff = Math.abs(angle - lastAngle);
        double angleDiffDiff = Math.abs(angleDiff - lastAngleDiff);

        if (deltaYaw > 3.5F && angleDiff <= 0.075) {
            if (++buffer > 5.0 && flagAndAlert(
                    "* Aimlock\np= " + deltaPitch
                            + "\ny= " + deltaYaw
                            + "\nang= " + angle
                            + "\nad= " + angleDiff
                            + "\nadd= " + angleDiffDiff)) {
                player.mitigateDamage();
            }
        } else {
            buffer = Math.max(0.0, buffer - 0.15);
        }

        lastAngle = angle;
        lastAngleDiff = angleDiff;
    }
}
