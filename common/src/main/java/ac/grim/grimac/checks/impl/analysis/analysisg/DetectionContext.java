package ac.grim.grimac.checks.impl.analysis.analysisg;

import ac.grim.grimac.checks.impl.analysis.AnalysisG;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.MathUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class DetectionContext {
    private final Queue<Rotation> rotationHistory = new ArrayDeque<>(100);
    private final Queue<Rotation> rotationDeltaHistory = new ArrayDeque<>(100);
    private final Queue<Double> reactionTimes = new ArrayDeque<>(50);
    private final GrimPlayer player;
    private ArrayList<Rotation> rotations;
    private double distanceToTarget;
    private Rotation currentRotation;
    private Rotation lastRotation;
    private float deltaYaw;
    private float deltaPitch;
    private float optimalYaw;
    private float avgYaw;
    private float avgPitch;
    private float maxYaw;
    private float maxPitch;
    private float lastDeltaYaw;
    private float lastDeltaPitch;
    private float lastOptimalYaw;
    private long lastMovementUpdateTime;
    private double previousYaw;
    private double previousPitch;
    private double previousYawVelocity;
    private double previousPitchVelocity;
    private double yawAcceleration;
    private double pitchAcceleration;

    public DetectionContext(GrimPlayer player) {
        this.player = player;
    }

    public void updateMovementData(double currentYaw, double currentPitch, long currentTime) {
        if (lastMovementUpdateTime == 0) {
            lastMovementUpdateTime = currentTime;
            previousYaw = currentYaw;
            previousPitch = currentPitch;
            return;
        }
        double deltaTime = (currentTime - lastMovementUpdateTime) / 1000.0;
        if (deltaTime <= 0) {
            return;
        }
        double currentYawVelocity = angularVelocity(previousYaw, currentYaw, deltaTime);
        double currentPitchVelocity = angularVelocity(previousPitch, currentPitch, deltaTime);
        yawAcceleration = (currentYawVelocity - previousYawVelocity) / deltaTime;
        pitchAcceleration = (currentPitchVelocity - previousPitchVelocity) / deltaTime;
        previousYawVelocity = currentYawVelocity;
        previousYaw = currentYaw;
        previousPitchVelocity = currentPitchVelocity;
        previousPitch = currentPitch;
        lastMovementUpdateTime = currentTime;
    }

    private double angularVelocity(double previousAngle, double currentAngle, double deltaTime) {
        double delta = currentAngle - previousAngle;
        delta = (delta + 180.0) % 360.0 - 180.0;
        return delta / deltaTime;
    }

    public void flagDetection(AimDetectionStrategy strategy, String reason) {
        AnalysisG check = player.checkManager.getCheck(AnalysisG.class);
        if (check != null && check.flagAndAlert(strategy.getCheckName() + " | " + reason)) {
            check.setbackIfAboveSetbackVL();
        }
    }

    public float getAngleDifference(float yaw1, float yaw2) {
        return Rotation.getAngleDifference(yaw1, yaw2);
    }

    public long getGcd(long a, long b) {
        return MathUtil.getGcd(a, b);
    }

    public void updateTargetInfo(double distance) {
        this.distanceToTarget = distance;
    }

    public void updateRotation(Rotation rotation) {
        this.lastRotation = this.currentRotation;
        this.currentRotation = rotation;
        if (rotationHistory.size() >= 100) {
            rotationHistory.poll();
        }
        rotationHistory.add(rotation);
    }

    public void updateRotationDelta(Rotation rotation) {
        if (rotationDeltaHistory.size() >= 100) {
            rotationDeltaHistory.poll();
        }
        rotationDeltaHistory.add(rotation);
        avgYaw = 0;
        avgPitch = 0;
        maxYaw = 0;
        maxPitch = 0;
        for (Rotation rot : rotationDeltaHistory) {
            avgYaw += Math.abs(rot.getYaw());
            avgPitch += Math.abs(rot.getPitch());
            maxYaw = Math.max(maxYaw, Math.abs(rot.getYaw()));
            maxPitch = Math.max(maxPitch, Math.abs(rot.getPitch()));
        }
        avgYaw /= rotationDeltaHistory.size();
        avgPitch /= rotationDeltaHistory.size();
    }

    public boolean isAimingAtTarget() {
        return distanceToTarget < 4.0;
    }

    public Queue<Rotation> getRotationHistory() {
        return rotationHistory;
    }

    public Queue<Rotation> getRotationDeltaHistory() {
        return rotationDeltaHistory;
    }

    public Queue<Double> getReactionTimes() {
        return reactionTimes;
    }

    public ArrayList<Rotation> getRotations() {
        return rotations;
    }

    public void setRotations(ArrayList<Rotation> rotations) {
        this.rotations = rotations;
    }

    public double getDistanceToTarget() {
        return distanceToTarget;
    }

    public Rotation getCurrentRotation() {
        return currentRotation;
    }

    public Rotation getLastRotation() {
        return lastRotation;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public void setDeltaYaw(float deltaYaw) {
        this.deltaYaw = deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public void setDeltaPitch(float deltaPitch) {
        this.deltaPitch = deltaPitch;
    }

    public float getOptimalYaw() {
        return optimalYaw;
    }

    public void setOptimalYaw(float optimalYaw) {
        this.optimalYaw = optimalYaw;
    }

    public float getAvgYaw() {
        return avgYaw;
    }

    public float getAvgPitch() {
        return avgPitch;
    }

    public float getMaxYaw() {
        return maxYaw;
    }

    public float getMaxPitch() {
        return maxPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public void setLastDeltaYaw(float lastDeltaYaw) {
        this.lastDeltaYaw = lastDeltaYaw;
    }

    public float getLastDeltaPitch() {
        return lastDeltaPitch;
    }

    public void setLastDeltaPitch(float lastDeltaPitch) {
        this.lastDeltaPitch = lastDeltaPitch;
    }

    public float getLastOptimalYaw() {
        return lastOptimalYaw;
    }

    public void setLastOptimalYaw(float lastOptimalYaw) {
        this.lastOptimalYaw = lastOptimalYaw;
    }

    public double getPreviousYawVelocity() {
        return previousYawVelocity;
    }

    public double getPreviousPitchVelocity() {
        return previousPitchVelocity;
    }

    public double getYawAcceleration() {
        return yawAcceleration;
    }

    public double getPitchAcceleration() {
        return pitchAcceleration;
    }
}
