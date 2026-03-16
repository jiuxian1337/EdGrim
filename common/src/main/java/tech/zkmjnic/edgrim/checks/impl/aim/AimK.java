package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimK", configName = "AimK", description = "Detects repeated rounded rotation steps", decay = 0.85)
public final class AimK extends EdAimCheck {
    public AimK(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float lastDeltaYaw = rotationUpdate.getProcessor().getLastDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        final float lastDeltaPitch = rotationUpdate.getProcessor().getLastDeltaPitch();

        final boolean cinematic = rotationUpdate.isCinematic();

        final boolean flag = deltaYaw > 0.32f
                && deltaPitch > 0.32f
                && deltaYaw < 20.f
                && deltaPitch < 20.f
                && hasAttackedSince(1100L)
                && !cinematic;

        if (flag) {
            final float rotationRound = Math.round(deltaYaw) + Math.round(deltaPitch);
            final float previousRotationRound = Math.round(lastDeltaYaw) + Math.round(lastDeltaPitch);

            if (rotationRound == previousRotationRound
                    && Math.round(deltaYaw) == Math.round(lastDeltaYaw)) {
                if (++buffer > 10) {
                    if (flagAndAlert("r= " + rotationRound + "\npr= " + previousRotationRound + "\ndy= " + rotationUpdate.getProcessor().getDeltaYaw() + "\ndp= " + rotationUpdate.getProcessor().getDeltaPitch())) {
                        mitigateDamage();
                        buffer = 0;
                    }
                }
            } else {
                rewardBufferAndVL();
            }
        }
    }
}
