package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimO", configName = "AimO", description = "Detects prolonged zero pitch delta with significant yaw", decay = 0.6)
public final class AimO extends EdAimCheck {
    private int zeroDeltaTicks;

    public AimO(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();

        final float pitch = rotationUpdate.getProcessor().getPitch();
        final float lastPitch = rotationUpdate.getProcessor().getLastPitch();

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            return;
        }

        if (hasAttackedSince(1000L)) {
            if (deltaPitch == 0.0F) {
                zeroDeltaTicks++;
            } else {
                zeroDeltaTicks = 0;
            }

            if (zeroDeltaTicks <= 40
                    || !(deltaYaw > 3.0F)
                    || !(Math.abs(pitch) < 45.0F)
                    || !(player.getDeltaXZ() > 0.08)) {
                buffer *= 0.75;
            } else if (buffer++ > 8.0) {
                if (flagAndAlert("now= " + pitch + "\nlast= " + lastPitch)) {
                    rewardBufferAndVL();
                    mitigateDamage();
                }
            }
        }
    }
}
