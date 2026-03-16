package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;

@CheckData(name = "AimP", configName = "AimP", description = "Detects invalid gcd and modulo patterns in rotations", decay = 0.84)
public final class AimP extends EdAimCheck {
    private static final double MODULO_THRESHOLD = 60.0;
    private static final double LINEAR_THRESHOLD = 0.1;
    private static final double GCD_MULTIPLIER = Math.pow(2, 24);
    private static final long GCD_THRESHOLD = 131_072L;
    private static final int BUFFER_MAX = 200;
    private static final int BUFFER_ALERT_THRESHOLD = 6;
    private static final int BUFFER_RESET_VALUE = 4;
    private double moduloCheckBuffer;

    public AimP(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!rotationUpdate.isCinematic()) {
            return;
        }
        checkType1(rotationUpdate);
        checkType2(rotationUpdate);
    }

    private void checkType1(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        final float lastDeltaPitch = rotationUpdate.getProcessor().getLastDeltaPitch();

        final long expandedPitch = (long) (GCD_MULTIPLIER * deltaPitch);
        final long expandedLastPitch = (long) (GCD_MULTIPLIER * lastDeltaPitch);

        final long gcd = MathUtil.getGcd(expandedPitch, expandedLastPitch);
        final boolean validSensitivity = calculateSensitivity() > 5;
        final boolean validRotation = isValidRotation(deltaYaw, deltaPitch);

        if (gcd < GCD_THRESHOLD && validRotation && !validSensitivity) {
            handleBufferIncrease();
        } else if (buffer > 0) {
            rewardBufferAndVL();
        }
    }

    private void checkType2(RotationUpdate rotationUpdate) {
        final float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        final float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        final float lastDeltaYaw = rotationUpdate.getProcessor().getLastDeltaYaw();
        final float lastDeltaPitch = rotationUpdate.getProcessor().getLastDeltaPitch();

        if (!isValidRotation(deltaYaw, deltaPitch)) {
            return;
        }

        final double yawConstant = calculateConstant(deltaYaw, lastDeltaYaw);
        final double pitchConstant = calculateConstant(deltaPitch, lastDeltaPitch);

        final double moduloX = calculateModulo(deltaYaw, yawConstant, lastDeltaYaw);
        final double moduloY = calculateModulo(deltaPitch, pitchConstant, lastDeltaPitch);

        final double floorModX = Math.abs(Math.floor(moduloX) - moduloX);
        final double floorModY = Math.abs(Math.floor(moduloY) - moduloY);

        if (isInvalidModulo(moduloX, floorModX) && isInvalidModulo(moduloY, floorModY)) {
            handleModuloBufferIncrease();
        } else if (moduloCheckBuffer > 0) {
            moduloCheckBuffer = Math.max(moduloCheckBuffer - 2, 0);
        }
    }

    private boolean isValidRotation(float dy, float dp) {
        return dy > 0.25f
                && dp > 0.25f
                && dy < 20.0f
                && dp < 20.0f;
    }

    private double calculateConstant(float current, float previous) {
        final long expandedCurrent = (long) (GCD_MULTIPLIER * current);
        final long expandedPrevious = (long) (GCD_MULTIPLIER * previous);
        return MathUtil.getGcd(expandedCurrent, expandedPrevious) / GCD_MULTIPLIER;
    }

    private double calculateModulo(float delta, double constant, float lastDelta) {
        final double current = delta / constant;
        final double previous = lastDelta / constant;
        return current % previous;
    }

    private boolean isInvalidModulo(double modulo, double floorMod) {
        return modulo > MODULO_THRESHOLD
                && floorMod > LINEAR_THRESHOLD;
    }

    private void handleBufferIncrease() {
        buffer = Math.min(buffer + 1, BUFFER_MAX);
        if (buffer > BUFFER_ALERT_THRESHOLD) {
            if (flagAndAlert("")) {
                buffer = BUFFER_RESET_VALUE;
            }
        }
    }

    private void handleModuloBufferIncrease() {
        moduloCheckBuffer = Math.min(moduloCheckBuffer + 1, BUFFER_MAX);
        if (moduloCheckBuffer > BUFFER_ALERT_THRESHOLD) {
            if (flagAndAlert("")) {
                moduloCheckBuffer = BUFFER_RESET_VALUE;
            }
        }
    }
}
