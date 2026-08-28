package cc.watchneko.checks.impl.aim.trajectory;

import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.math.MathUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class FrictionDetection implements AimDetectionStrategy {
    private final Queue<Double> yawVelocities = new ArrayDeque<>(10);
    private final Queue<Double> pitchVelocities = new ArrayDeque<>(10);
    private final Queue<Double> yawDeltas = new ArrayDeque<>(20);
    private final Queue<Double> pitchDeltas = new ArrayDeque<>(20);
    private double buffer1 = 0;
    private double buffer2 = 0;
    private double buffer3 = 0;
    private double lastYawVelocity = 0;
    private double lastPitchVelocity = 0;
    private double lastYawAcceleration = 0;
    private double lastPitchAcceleration = 0;

    @Override
    public void detect(PlayerData profile, DetectionContext context) {
        double currentYawVelocity = context.getPreviousYawVelocity();
        double currentPitchVelocity = context.getPreviousPitchVelocity();
        double currentYawAcceleration = context.getYawAcceleration();
        double currentPitchAcceleration = context.getPitchAcceleration();
        int senv = profile.calculateSensitivity();

        yawVelocities.add(currentYawVelocity);
        pitchVelocities.add(currentPitchVelocity);
        if (yawVelocities.size() > 15) yawVelocities.poll();
        else return;
        if (pitchVelocities.size() > 15) pitchVelocities.poll();
        else return;

        double decelerationYaw = 0;
        double decelerationPitch = 0;
        boolean flagged = false;
        if (Math.abs(lastYawVelocity) > 30 && Math.abs(currentYawVelocity) < 5) {
            double expectedDeceleration = -lastYawVelocity;
            decelerationYaw = Math.abs(currentYawVelocity - expectedDeceleration);
            if (decelerationYaw > 200) {
                flagged = true;
            }
        } else if (Math.abs(lastPitchVelocity) > 30 && Math.abs(currentPitchVelocity) < 5) {
            double expectedDeceleration = -lastPitchVelocity;
            decelerationPitch = Math.abs(currentPitchVelocity - expectedDeceleration);
            if (decelerationPitch > 200) {
                flagged = true;
            }
        }
        if (flagged) {
            if (buffer1++ > 2) {
                // keep original commented branch
            }
        } else {
            buffer1 = Math.max(buffer1 - 0.02, 0);
        }

        if ((Math.abs(currentYawVelocity) > 100 || Math.abs(currentPitchVelocity) > 100)) {
            double correlation = MathUtil.pearsonCorrelation(
                    yawVelocities.stream().mapToDouble(Double::doubleValue).toArray(),
                    pitchVelocities.stream().mapToDouble(Double::doubleValue).toArray()
            );
            if (Math.abs(correlation) < (senv > 80 ? -0.1 : -0.4)) {
                buffer2 += 1.2;
                if (buffer2 > 6) {
                    buffer2 -= 2;
                    context.flagDetection(this, String.format("patterned #1: %.2f, v2: %.2f, c: %.2f", currentYawVelocity, currentPitchVelocity, correlation));
                }
            } else {
                buffer2 = Math.max(0, buffer2 - 1);
            }
        } else {
            buffer2 = Math.max(0, buffer2 - 0.7);
        }

        double deltaY = Math.abs(currentYawAcceleration - lastYawAcceleration);
        double deltaX = Math.abs(currentPitchAcceleration - lastPitchAcceleration);
        if (deltaY > 1 && deltaX > 1) {
            yawDeltas.add(deltaY);
            pitchDeltas.add(deltaX);
        }
        if (yawDeltas.size() > 15) yawDeltas.poll();
        if (pitchDeltas.size() > 15) pitchDeltas.poll();

        if (yawDeltas.size() == 15 && pitchDeltas.size() == 15) {
            ArrayList<String> tags = new ArrayList<>();
            double limit3 = senv < 80 ? 3500 : 5000;
            if (deltaX > limit3 || deltaY > limit3) {
                buffer3 += 0.1;
                tags.add("large");
            }
            if (deltaX > 0 && deltaY > 0) {
                double ratioXY = deltaX / deltaY;
                double ratioYX = deltaY / deltaX;
                double limit = senv < 80 ? 80 : 65;
                if (ratioXY > limit || ratioYX > limit) {
                    buffer3 += 2;
                    tags.add("not harmonize, r=" + String.format("%.3f, %.3f", ratioXY, ratioYX));
                }
            }

            double frictionScore = calculateFrictionScore();
            double limit2 = senv < 80 ? -0.8 : -0.2;
            if (frictionScore < limit2) {
                buffer3 += 0.8;
                tags.add("low score, s=" + String.format("%.3f", frictionScore));
            }
            if (hasAbruptChange(yawDeltas) || hasAbruptChange(pitchDeltas)) {
                buffer3 += 0.2;
                tags.add("abrupt");
            }
            if (buffer3 >= 20) {
                buffer3 -= 4;
                context.flagDetection(this, String.format("combined friction dy: %.2f, dx: %.2f, tags=[%s]", deltaY, deltaX, String.join(",", tags)));
            }
        }

        buffer3 = Math.max(0, buffer3 - 1);
        lastYawVelocity = currentYawVelocity;
        lastPitchVelocity = currentPitchVelocity;
        lastYawAcceleration = currentYawAcceleration;
        lastPitchAcceleration = currentPitchAcceleration;
    }

    private double calculateFrictionScore() {
        double meanX = calculateMean(yawDeltas);
        double meanY = calculateMean(pitchDeltas);
        double stdDevX = calculateStdDev(yawDeltas, meanX);
        double stdDevY = calculateStdDev(pitchDeltas, meanY);
        double cvX = (meanX != 0) ? stdDevX / meanX : 0;
        double cvY = (meanY != 0) ? stdDevY / meanY : 0;
        return 1.0 - Math.max(cvX, cvY);
    }

    private boolean hasAbruptChange(Queue<Double> history) {
        if (history.size() < 2) return false;
        Double[] deltas = history.toArray(new Double[0]);
        for (int i = 1; i < deltas.length; i++) {
            double change = Math.abs(deltas[i] - deltas[i - 1]);
            if (change > 3000.0) {
                return true;
            }
        }
        return false;
    }

    private double calculateMean(Queue<Double> data) {
        return data.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double calculateStdDev(Queue<Double> data, double mean) {
        double variance = data.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    @Override
    public String getCheckName() {
        return "Friction";
    }

    @Override
    public void changeTarget() {
    }

    @Override
    public void reset() {
        yawVelocities.clear();
        pitchVelocities.clear();
        yawDeltas.clear();
        pitchDeltas.clear();
    }
}
