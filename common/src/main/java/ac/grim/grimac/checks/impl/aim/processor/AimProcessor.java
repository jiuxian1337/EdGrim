package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.lists.RunningMode;
import ac.grim.grimac.utils.math.GrimMath;

public class AimProcessor extends Check implements RotationCheck {

    private static final int SIGNIFICANT_SAMPLES_THRESHOLD = 15;
    private static final int TOTAL_SAMPLES_THRESHOLD = 80;
    public double sensitivityX;
    public double sensitivityY;
    public double divisorX;
    public double divisorY;
    public double modeX, modeY;
    public double deltaDotsX, deltaDotsY;
    private final RunningMode xRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
    private final RunningMode yRotMode = new RunningMode(TOTAL_SAMPLES_THRESHOLD);
    private float lastXRot;
    private float lastYRot;
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private float deltaYaw;
    private float deltaPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private float yawAccel;
    private float pitchAccel;
    private float lastYawAccel;
    private float lastPitchAccel;
    private double avgYaw;
    private double avgPitch;
    public int totalSensitivityClient;

    public AimProcessor(GrimPlayer playerData) {
        super(playerData);
    }

    public static double convertToSensitivity(double var13) {
        double var11 = var13 / 0.15F / 8.0D;
        double var9 = Math.cbrt(var11);
        return (var9 - 0.2f) / 0.6f;
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        rotationUpdate.setProcessor(this);

        lastYaw = yaw;
        lastPitch = pitch;
        yaw = rotationUpdate.getTo().getYaw();
        pitch = rotationUpdate.getTo().getPitch();

        lastDeltaYaw = deltaYaw;
        lastDeltaPitch = deltaPitch;
        deltaYaw = Math.abs(yaw - lastYaw);
        deltaPitch = Math.abs(pitch - lastPitch);

        lastYawAccel = yawAccel;
        lastPitchAccel = pitchAccel;
        yawAccel = Math.abs(deltaYaw - lastDeltaYaw);
        pitchAccel = Math.abs(deltaPitch - lastDeltaPitch);

        avgYaw = yaw;
        avgPitch = pitch;

        float deltaXRot = rotationUpdate.getDeltaXRotABS();

        this.divisorX = GrimMath.gcd(deltaXRot, lastXRot);
        if (deltaXRot > 0 && deltaXRot < 5 && divisorX > GrimMath.MINIMUM_DIVISOR) {
            this.xRotMode.add(divisorX);
            this.lastXRot = deltaXRot;
        }

        float deltaYRot = rotationUpdate.getDeltaYRotABS();

        this.divisorY = GrimMath.gcd(deltaYRot, lastYRot);

        if (deltaYRot > 0 && deltaYRot < 5 && divisorY > GrimMath.MINIMUM_DIVISOR) {
            this.yRotMode.add(divisorY);
            this.lastYRot = deltaYRot;
        }

        if (this.xRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modeX = this.xRotMode.getMode();
            if (modeX.second() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modeX = modeX.first();
                this.sensitivityX = convertToSensitivity(this.modeX);
            }
        }
        if (this.yRotMode.size() > SIGNIFICANT_SAMPLES_THRESHOLD) {
            Pair<Double, Integer> modeY = this.yRotMode.getMode();
            if (modeY.second() > SIGNIFICANT_SAMPLES_THRESHOLD) {
                this.modeY = modeY.first();
                this.sensitivityY = convertToSensitivity(this.modeY);
            }
        }

        this.deltaDotsX = deltaXRot / modeX;
        this.deltaDotsY = deltaYRot / modeY;
        double sensitivity = Math.max(sensitivityX, sensitivityY);
        if (Double.isNaN(sensitivity) || Double.isInfinite(sensitivity)) {
            totalSensitivityClient = -1;
        } else {
            totalSensitivityClient = (int) Math.round(sensitivity * 200);
        }
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public float getLastDeltaPitch() {
        return lastDeltaPitch;
    }

    public float getYawAccel() {
        return yawAccel;
    }

    public float getPitchAccel() {
        return pitchAccel;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public double getAvgYaw() {
        return avgYaw;
    }

    public double getAvgPitch() {
        return avgPitch;
    }
}
