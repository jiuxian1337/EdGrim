package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimH", configName = "AimH", decay = 0.65, description = "Detects low yaw with high pitch changes during attacks")
public final class AimH extends EdAimCheck {
    int maxBuffer;
    double minDeltaY;
    double maxDeltaX;
    double dynamicMaxDeltaX;
    double dynamicMinDeltaY;

    public AimH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getProcessor().getDeltaYaw();
        double deltaY = rotationUpdate.getProcessor().getDeltaPitch();

        if (!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) return;

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            return;
        }

        dynamicMaxDeltaX = calculateDynamicMaxDeltaX();
        dynamicMinDeltaY = calculateDynamicMinDeltaY();

        if (!hasAttackedSince(250L)) {
            return;
        }

        if (deltaX < dynamicMaxDeltaX && deltaY > dynamicMinDeltaY && shouldModifyPackets()) {
            if (buffer++ > maxBuffer) {
                if (flagAndAlert("deltaX= " + deltaX
                        + "\ndeltaY= " + deltaY)) {
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
        minDeltaY = config.getDoubleElse(getConfigName() + ".min-deltaY", 1D);
        maxDeltaX = config.getDoubleElse(getConfigName() + ".max-deltaX", 0.0001D);
    }

    private double calculateDynamicMaxDeltaX() {
        return maxDeltaX * (1 + Math.sin(time() / 1000.0));
    }

    private double calculateDynamicMinDeltaY() {
        return minDeltaY * (1 + Math.cos(time() / 1000.0));
    }
}
