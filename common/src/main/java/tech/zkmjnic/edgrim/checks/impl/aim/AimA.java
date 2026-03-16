package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@CheckData(name = "AimA", configName = "AimA", decay = 0.75, description = "Detects repetitive angle step patterns in attack rotations")
public final class AimA extends EdAimCheck {
    private final List<Double> stack = new LinkedList<>();

    public AimA(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (rotationUpdate.isCinematic()) return;
        if (!hasAttackedSince(500L)) return;

        double deltaX = rotationUpdate.getProcessor().getDeltaYaw();
        double deltaY = rotationUpdate.getProcessor().getDeltaPitch();

        if (player.getTarget() != null && player.getTarget().type != EntityTypes.PLAYER) {
            return;
        }

        if (deltaY == 0 && deltaX == 0) return;

        if (Math.abs(rotationUpdate.getTo().getPitch()) >= 90) return;

        if (isExempt(ExemptType.TELEPORT, ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE, ExemptType.ELYTRA_FLYING, ExemptType.VEHICLE)) {
            return;
        }

        Vec2f delta = rotationUpdate.getDelta();
        double angle = MathUtil.getAngleInDegrees(delta) % 90;

        if ((deltaY > 1.5 && deltaX > 0.32) || deltaX > 1.5) {
            stack.add(angle);
        }

        if (stack.size() >= 20) {
            List<Float> jiff = MathUtil.getJiffDelta(stack, 1);
            float prev = 999f;
            float prePrev = 999f;

            for (float current : jiff) {
                if (current == 0f && prev == 0f && prePrev == 0f) {
                    if (buffer++ > 5) {
                        if (flagAndAlert("m= " + Arrays.toString(jiff.toArray()))) {
                            mitigateDamage();
                        }
                    } else {
                        rewardBufferAndVL();
                    }
                    break;
                }
                prePrev = prev;
                prev = current;
            }

            stack.clear();
        }
    }
}
