package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import tech.zkmjnic.edgrim.utils.lists.EvictingList;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.List;

@CheckData(
        name = "AimFocus",
        description = "stable optimal-yaw deviation",
        experimental = true
)
public final class AimFocus extends Check implements RotationCheck {
    private final List<Double> differenceSamples = new EvictingList<>(25);

    public AimFocus(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        PacketEntity target = AimTargetTraceUtil.getPlayerTarget(player);
        if (target == null || target.type != EntityTypes.PLAYER || !player.actionManager.hasAttackedSince(300L)) {
            return;
        }

        final float deltaYaw = update.getDeltaYRotABS();
        final float rotationYaw = update.getTo().getYaw();
        final float fixedRotYaw = (rotationYaw % 360F + 360F) % 360F;
        final double optimalYaw = (AimTargetTraceUtil.directionToCenter(player, AimTargetTraceUtil.getTargetBox(target)) % 360.0 + 360.0) % 360.0;
        final double difference = Math.abs(fixedRotYaw - optimalYaw);

        if (deltaYaw > 3f) {
            differenceSamples.add(difference);
        }

        if (differenceSamples.size() == 25) {
            final double average = MathUtil.getAverageDouble(differenceSamples);
            final double deviation = MathUtil.getStandardDeviation(differenceSamples);
            if (average < 7 && deviation < 12) {
                if (++buffer > 15 && flagAndAlert(String.format("dev=%.2f, avg=%.2f, buf=%.2f", deviation, average, buffer))) {
                    player.mitigateDamage();
                }
            } else {
                buffer -= buffer > 0 ? 1 : 0;
            }
        }
    }
}
