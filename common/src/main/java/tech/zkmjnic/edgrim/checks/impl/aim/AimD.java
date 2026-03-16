package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.checks.impl.aim.processor.AimProcessor;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.lists.EvictingList;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.math.Vec2f;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import java.util.List;

@CheckData(name = "AimD", configName = "AimD", decay = 0.95, description = "Detects repeated rotation triplets and unnatural flick patterns")
public final class AimD extends EdAimCheck {
    private final List<Double> stack = new EvictingList<>(3);
    private boolean lastIsNoRotation = false;
    private double lastHash = 0;
    private float localBuffer = 0;
    private int ticksToReset = 0;

    public AimD(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (rotationUpdate.isCinematic()) {
            stack.clear();
            localBuffer = 0;
            return;
        }
        if (hasAttackedSince(800L)) {

            if (player.getTarget() != null && player.getTarget().type == EntityTypes.PLAYER) {
                if (rotationUpdate.getDelta().getY() == 0 && rotationUpdate.getDelta().getX() == 0) {
                    if (!lastIsNoRotation) stack.add(0.0);
                    check();
                    lastIsNoRotation = true;
                } else {
                    Vec2f delta = rotationUpdate.getDelta();
                    stack.add(MathUtil.scaleVal(delta.getX(), 2));
                    check();
                    lastIsNoRotation = false;
                }
            }
        }
    }

    private void check() {
        if (isExempt(ExemptType.RESPAWN, ExemptType.TELEPORT)) {
            stack.clear();
        }
        AimProcessor processor = rotationUpdateProcessor();
        if (processor == null) return;
        if (processor.getDeltaYaw() > 40 || processor.getDeltaPitch() > 45) {
            return;
        }
        if (stack.size() != 3) return;
        double hash = stack.get(0) + stack.get(1) + stack.get(2);
        if (hash == lastHash) return;
        double centre = stack.get(1);
        boolean hugeRotation = centre > 35;
        if (hugeRotation && centre != 360.0f) {
            double compare = 110;
            boolean invalid = (stack.get(0) < compare && stack.get(2) < compare)
                    || MathUtil.getMax(stack) > 70 && MathUtil.getMin(stack) < compare && MathUtil.getDistinct(stack) != 3;
            if (invalid) {
                float localVl = (centre > 160) ? 3 : (centre < 60) ? 1 : 2;
                localBuffer += localVl;
                if (localBuffer >= 8) {
                    if (flagAndAlert("(" + centre + "/" + MathUtil.scaleVal(stack.get(0) + stack.get(2), 2) + ")")) {
                        mitigateDamage();
                    }
                }
            } else {
                rewardBufferAndVL();
            }
        } else {
            ticksToReset++;
            if (ticksToReset >= 2500) {
                ticksToReset = 0;
                localBuffer = 0;
            }
        }
        lastHash = hash;
    }

    private AimProcessor rotationUpdateProcessor() {
        return player.checkManager.getRotationCheck(AimProcessor.class);
    }
}
