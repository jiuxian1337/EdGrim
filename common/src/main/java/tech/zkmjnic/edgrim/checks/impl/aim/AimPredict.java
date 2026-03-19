package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import tech.zkmjnic.edgrim.utils.lists.EvictingList;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CheckData(
        name = "AimPredict",
        description = "predictive target tracking"
)
public final class AimPredict extends Check implements RotationCheck {
    private final List<Float> pitchMatches = new EvictingList<>(100);
    private final List<Float> yawMatches = new EvictingList<>(100);
    private UUID lastTargetId;
    private SimpleCollisionBox lastTargetBox;

    public AimPredict(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        PacketEntity target = AimTargetTraceUtil.getPlayerTarget(player);
        if (target == null || target.type != EntityTypes.PLAYER || target.getUuid() == null) {
            resetTarget();
            return;
        }

        if (!player.actionManager.hasAttackedSince(50L) || player.getDeltaXZ() <= 0.1) {
            return;
        }

        SimpleCollisionBox currentBox = AimTargetTraceUtil.getTargetBox(target);
        if (!target.getUuid().equals(lastTargetId) || lastTargetBox == null) {
            lastTargetId = target.getUuid();
            lastTargetBox = currentBox.copy();
            return;
        }

        if (AimTargetTraceUtil.centerDistance(currentBox, lastTargetBox) >= 0.03125D) {
            Vec2f rotation = AimTargetTraceUtil.predictiveRotations(player.lastX, player.lastY, player.lastZ, currentBox, lastTargetBox);
            if (update.getDeltaYRotABS() > 0.0F) {
                yawMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.getX(), update.getTo().getYaw())));
            }
            if (update.getDeltaXRotABS() > 0.0F) {
                pitchMatches.add(Math.abs(MathUtil.getAngleDifference(rotation.getY(), update.getTo().getPitch())));
            }
        }

        if (yawMatches.size() == 100) {
            List<Float> close = yawMatches.stream().filter(delta -> delta <= 1.5F).collect(Collectors.toList());
            if (close.size() >= 75 && flagAndAlert("* Rotation predict (generic, yaw)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                player.mitigateDamage();
            }
            yawMatches.clear();
        }

        if (pitchMatches.size() == 100) {
            List<Float> close = pitchMatches.stream().filter(delta -> delta <= 1.0F).collect(Collectors.toList());
            if (close.size() >= 75 && flagAndAlert("* Rotation predict (generic, pitch)\navg= " + MathUtil.getAverage(close) + "\nrate= " + close.size())) {
                player.mitigateDamage();
            }
            pitchMatches.clear();
        }

        lastTargetId = target.getUuid();
        lastTargetBox = currentBox.copy();
    }

    private void resetTarget() {
        lastTargetId = null;
        lastTargetBox = null;
        yawMatches.clear();
        pitchMatches.clear();
    }
}
