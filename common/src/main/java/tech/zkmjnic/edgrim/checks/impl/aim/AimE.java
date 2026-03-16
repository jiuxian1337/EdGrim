package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

@CheckData(name = "AimE", configName = "AimE", description = "Detects aim randomizer flaws in large rotation corrections", decay = 0.05)
public final class AimE extends EdAimCheck {
    public AimE(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (hasAttackedSince(500L)) {

            if (isExempt(ExemptType.VEHICLE, ExemptType.VEHICLE_SWITCH, ExemptType.TELEPORT)) {
                buffer = 0;
            }

            float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
            float optimalYaw = rotationUpdate.getProcessor().getYaw();
            float lastYaw = rotationUpdate.getProcessor().getLastYaw();
            float lastOptimalYaw = rotationUpdate.getProcessor().getLastYaw();

            float distOld = MathUtil.getAngleDifference(lastYaw, lastOptimalYaw);
            float dist = MathUtil.getAngleDifference(rotationUpdate.getTo().getYaw(), optimalYaw);

            if (deltaYaw > 40 && distOld > 26 && dist < 15) {
                if (buffer++ > 3) {
                    if (flagAndAlert("old= " + distOld + "\nnow= " + dist)) {
                        mitigateDamage();
                    }
                }
            } else {
                rewardBufferAndVL();
            }
        }
    }
}
