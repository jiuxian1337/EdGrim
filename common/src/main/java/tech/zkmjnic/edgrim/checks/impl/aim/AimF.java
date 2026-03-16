package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

@CheckData(name = "AimF", configName = "AimF", decay = 0.65, setback = 5, description = "Detects low-accel rotations with invalid angle GCD")
public final class AimF extends EdAimCheck {
    private double minDeltaX;
    private double maxDeltaXAccel;
    private int maxBuffer;
    private double maxRotationAngle;

    public AimF(EdGrimPlayer player) {
        super(player);
        buffer = 0;
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getDeltaXRotABS();
        double deltaXAccel = rotationUpdate.getProcessor().getYawAccel();

        if (!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
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

        if (rotationUpdate.isCinematic()) {
            return;
        }

        double rotationAngle = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(rotationUpdate.getDeltaYRotABS(), 2));

        double gcd = MathUtil.getGcd(rotationAngle, maxRotationAngle);
        rotationAngle = rotationAngle / gcd;

        if (hasAttackedSince(150L)) {
            if (rotationAngle < maxRotationAngle && deltaXAccel < maxDeltaXAccel && deltaX > minDeltaX) {
                if (buffer++ > maxBuffer) {
                    if (flagAndAlert("angle= " + rotationAngle + "\ndeltaX= " + deltaX)) {
                        if (isAboveSetbackVl()) {
                            mitigateDamage();
                        }
                    }
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
        maxDeltaXAccel = config.getDoubleElse(getConfigName() + ".max-deltaXAccel", 0.1D);
        maxRotationAngle = config.getDoubleElse(getConfigName() + ".max-rotation-angle", 5.0D);
    }
}
