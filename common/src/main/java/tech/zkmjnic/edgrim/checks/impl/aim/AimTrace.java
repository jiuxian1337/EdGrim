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
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.List;
import java.util.stream.Collectors;

@CheckData(
        name = "AimTrace",
        description = "generic target rotation matching"
)
public final class AimTrace extends Check implements RotationCheck {
    private final List<Float> pitchMatches = new EvictingList<>(100);
    private final List<Float> yawMatches = new EvictingList<>(100);
    private double yawBuffer;
    private double pitchBuffer;

    public AimTrace(PlayerData player) {
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

        Vec2f rotation = AimTargetTraceUtil.genericRotations(player.lastX, player.lastY, player.lastZ, AimTargetTraceUtil.getTargetBox(target));
        if (update.getDeltaYRotABS() > 0.0F) {
            yawMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.getX(), update.getTo().getYaw())));
        }
        if (update.getDeltaXRotABS() > 0.0F) {
            pitchMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.getY(), update.getTo().getPitch())));
        }

        if (yawMatches.size() == 100) {
            List<Float> close = yawMatches.stream().filter(delta -> delta <= 1.5F).collect(Collectors.toList());
            if (close.size() >= 70) {
                if (++yawBuffer > 1.0 && flagAndAlert("* Rotation trace (generic, yaw)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                    player.mitigateDamage();
                }
            } else {
                yawBuffer = Math.max(0.0, yawBuffer - 0.05);
            }
            yawMatches.clear();
        }

        if (pitchMatches.size() == 100) {
            List<Float> close = pitchMatches.stream().filter(delta -> delta <= 1.5F).collect(Collectors.toList());
            if (close.size() >= 70) {
                if (++pitchBuffer > 1.0 && flagAndAlert("* Rotation trace (generic, pitch)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                    player.mitigateDamage();
                }
            } else {
                pitchBuffer = Math.max(0.0, pitchBuffer - 0.05);
            }
            pitchMatches.clear();
        }
    }
}
