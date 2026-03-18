package tech.zkmjnic.edgrim.checks.impl.aim.processor;

import lombok.Getter;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.Pair;
import tech.zkmjnic.edgrim.utils.lists.RunningMode;
import tech.zkmjnic.edgrim.utils.math.GrimMath;

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
    @Getter
    private float yaw;
    @Getter
    private float pitch;
    @Getter
    private float lastYaw;
    @Getter
    private float lastPitch;
    @Getter
    private float deltaYaw;
    @Getter
    private float deltaPitch;
    @Getter
    private float lastDeltaYaw;
    @Getter
    private float lastDeltaPitch;
    @Getter
    private float yawAccel;
    @Getter
    private float pitchAccel;
    private float lastYawAccel;
    private float lastPitchAccel;
    @Getter
    private double avgYaw;
    @Getter
    private double avgPitch;
    public int totalSensitivityClient;

    public AimProcessor(EdGrimPlayer playerData) {
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

}
