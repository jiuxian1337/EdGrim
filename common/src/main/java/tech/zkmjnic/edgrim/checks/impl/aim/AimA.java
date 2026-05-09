package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.aim.util.AimTargetTraceUtil;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import tech.zkmjnic.edgrim.utils.lists.EvictingList;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.List;
import java.util.stream.Collectors;

@CheckData(
        name = "AimA",
        description = "common target box alignment"
)
public final class AimA extends Check implements RotationCheck {
    private final List<Float> pitchMatches = new EvictingList<>(150);
    private final List<Float> yawMatches = new EvictingList<>(150);

    public AimA(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        PacketEntity target = AimTargetTraceUtil.getPlayerTarget(player);
        if (target == null || target.type != EntityTypes.PLAYER) {
            return;
        }

        if (!player.actionManager.hasAttackedSince(50L) || player.getDeltaXZ() <= 0.1) {
            return;
        }

        Vec2f rotation = AimTargetTraceUtil.commonRotations(player.lastX, player.lastY, player.lastZ, AimTargetTraceUtil.getTargetBox(target));
        if (update.getDeltaYRotABS() > 0.0F) {
            yawMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.x(), update.getTo().getYaw())));
        }
        if (update.getDeltaXRotABS() > 0.0F) {
            pitchMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.y(), update.getTo().getPitch())));
        }

        if (yawMatches.size() == 150) {
            List<Float> close = yawMatches.stream().filter(delta -> delta <= 2.0F).collect(Collectors.toList());
            if (close.size() >= 110 && flagAndAlert("* Rotation align (common, yaw)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                player.mitigateDamage();
            }
            yawMatches.clear();
        }

        if (pitchMatches.size() == 150) {
            List<Float> close = pitchMatches.stream().filter(delta -> delta <= 2.0F).collect(Collectors.toList());
            if (close.size() >= 110 && flagAndAlert("* Rotation align (common, pitch)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                player.mitigateDamage();
            }
            pitchMatches.clear();
        }
    }
}
