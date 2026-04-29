package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

@CheckData(
        name = "AimF",
        configName = "AimF",
        description = "invalid rotation gcd change",
        setback = 8,
        decay = 0.95
)
public final class AimF extends Check implements RotationCheck {

    public AimF(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        if (!player.actionManager.hasAttackedSince(1000L) || update.isCinematic2() || !player.isMoving()) {
            return;
        }

        if (player.packetStateData.lastPacketWasTeleport
                || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.compensatedEntities.self.getRiding() != null) {
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
            final String info = String.format("mx= %.2f\nmy= %.2f\nfmx= %.2f\nfmy= %.2f", moduloX, moduloY, floorModuloX, floorModuloY);

            if (invalidX && invalidY) {
                if (buffer++ > 6 && flagAndAlert(info)) {
                    player.mitigateDamage();
                }
            } else {
                rewardBufferAndVL();
            }
        }
    }
}
