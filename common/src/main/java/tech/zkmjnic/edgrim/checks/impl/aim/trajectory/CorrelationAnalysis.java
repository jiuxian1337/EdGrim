package tech.zkmjnic.edgrim.checks.impl.aim.trajectory;

import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.ArrayDeque;
import java.util.Queue;

public class CorrelationAnalysis implements StatisticalDetectionStrategy {
    private final Queue<Double> yawVelocities = new ArrayDeque<>(50);
    private final Queue<Double> pitchVelocities = new ArrayDeque<>(50);
    double buffer = 0;

    @Override
    public void detect(PlayerData profile, DetectionContext context) {
        double deltaYaw = context.getDeltaYaw();
        double deltaPitch = context.getDeltaPitch();
        long deltaTime = System.currentTimeMillis();
        double yawVelocity = deltaTime > 0 ? deltaYaw / (deltaTime / 1000.0) : 0;
        double pitchVelocity = deltaTime > 0 ? deltaPitch / (deltaTime / 1000.0) : 0;
        yawVelocities.add(yawVelocity);
        pitchVelocities.add(pitchVelocity);
        if (yawVelocities.size() < 50) return;

        double correlation = calculateCorrelation(yawVelocities.toArray(new Double[0]), pitchVelocities.toArray(new Double[0]));
        double acc = Math.abs(correlation);
        if (acc < 0.2 && isFastMovement()) {
            if (buffer++ > 5) context.flagDetection(this, String.format("p=%.3f", correlation));
        } else {
            buffer -= 0.6;
        }

        yawVelocities.poll();
        pitchVelocities.poll();
    }

    private double calculateCorrelation(Double[] x, Double[] y) {
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
        int n = x.length;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }
        double numerator = sumXY - (sumX * sumY / n);
        double denominator = Math.sqrt((sumX2 - sumX * sumX / n) * (sumY2 - sumY * sumY / n));
        return denominator == 0 ? 0 : numerator / denominator;
    }

    private boolean isFastMovement() {
        double maxYaw = 0;
        for (Double yaw : yawVelocities) {
            if (Math.abs(yaw) > maxYaw) {
                maxYaw = Math.abs(yaw);
            }
        }
        return maxYaw > 3;
    }

    @Override
    public String getCheckName() {
        return "Cross Axis";
    }

    @Override
    public void changeTarget() {
    }

    @Override
    public void reset() {
        yawVelocities.clear();
        pitchVelocities.clear();
    }
}
