package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimJ", configName = "AimJ", decay = 0.76, description = "Detects low yaw acceleration with large yaw steps")
public final class AimJ extends EdAimCheck {
    private int maxBuffer;
    private double minDeltaX;
    private double maxDeltaXAccel;

    public AimJ(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final double deltaYawAccel = rotationUpdate.getProcessor().getYawAccel();
        final double deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();

        if (Math.abs(rotationUpdate.getTo().getPitch()) == 90) {
            return;
        }

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            return;
        }

        if (player.getRespawnTick() < 10 || player.getSinceRiptideSpinTick() < 60 || player.packetStateData.horseInteractCausedForcedRotation) {
            return;
        }

        if (deltaYawAccel <= maxDeltaXAccel && deltaYaw >= minDeltaX) {
            if (buffer++ > maxBuffer) {
                if (flagAndAlert("accelX= " + deltaYawAccel + "\nrotX= " + deltaYaw)) {
                    mitigateDamage();
                    buffer = 0;
                }
            }
        } else {
            rewardBufferAndVL();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getIntElse(getConfigName() + ".buffer", 7);
        minDeltaX = config.getDoubleElse(getConfigName() + ".min-deltaX", 0.4D);
        maxDeltaXAccel = config.getDoubleElse(getConfigName() + ".max-deltaX-accel", 0.0001D);
    }
}
