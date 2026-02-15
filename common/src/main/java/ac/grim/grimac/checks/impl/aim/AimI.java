package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.MathUtil;

@CheckData(name = "AimI", configName = "AimI", description = "Detects invalid gcd transitions in small rotations", decay = 0.95, setback = 8)
public final class AimI extends EdAimCheck {
    public AimI(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!hasAttackedSince(1000L)) {
            return;
        }

        if (update.isCinematic()) return;

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE) || player.packetStateData.horseInteractCausedForcedRotation
                || !isMoving()) {
            return;
        }

        final float deltaYaw = update.getProcessor().getDeltaYaw() % 360F;
        final float deltaPitch = update.getProcessor().getDeltaPitch();

        final float lastDeltaYaw = update.getProcessor().getLastDeltaYaw() % 360F;
        final float lastDeltaPitch = update.getProcessor().getLastDeltaPitch();

        final double divisorYaw = MathUtil.getGcd((long) (deltaYaw * MathUtil.EXPANDER), (long) (lastDeltaYaw * MathUtil.EXPANDER));
        final double divisorPitch = MathUtil.getGcd((long) (deltaPitch * MathUtil.EXPANDER), (long) (lastDeltaPitch * MathUtil.EXPANDER));

        final double constantYaw = divisorYaw / MathUtil.EXPANDER;
        final double constantPitch = divisorPitch / MathUtil.EXPANDER;

        final double currentX = deltaYaw / constantYaw;
        final double currentY = deltaPitch / constantPitch;

        final double previousX = lastDeltaYaw / constantYaw;
        final double previousY = lastDeltaPitch / constantPitch;

        if (deltaYaw > 0.0 && deltaPitch > 0.0 && deltaYaw < 20.f && deltaPitch < 20.f) {
            final double moduloX = currentX % previousX;
            final double moduloY = currentY % previousY;

            final double floorModuloX = Math.abs(Math.floor(moduloX) - moduloX);
            final double floorModuloY = Math.abs(Math.floor(moduloY) - moduloY);

            final boolean invalidX = moduloX > 90.d && floorModuloX > 0.1;
            final boolean invalidY = moduloY > 90.d && floorModuloY > 0.1;

            final String info = String.format(
                    "mx= %.2f\nmy= %.2f\nfmx= %.2f\nfmy= %.2f",
                    moduloX, moduloY, floorModuloX, floorModuloY
            );

            if (invalidX && invalidY) {
                if (buffer++ > 6) {
                    if (flagAndAlert(info)) {
                        mitigateDamage();
                    }
                }
            } else {
                rewardBufferAndVL();
            }
        }
    }
}
