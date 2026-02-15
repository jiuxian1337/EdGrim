package ac.grim.grimac.checks.impl.analysis.analysisg;

import ac.grim.grimac.player.GrimPlayer;

import java.util.ArrayDeque;
import java.util.Queue;

public class CorrelationAnalysis implements StatisticalDetectionStrategy {
    private final Queue<Double> yawVelocities = new ArrayDeque<>(50);
    private final Queue<Double> pitchVelocities = new ArrayDeque<>(50);
    private double buffer;

    @Override
    public void detect(GrimPlayer player, DetectionContext context) {
        double deltaYaw = context.getDeltaYaw();
        double deltaPitch = context.getDeltaPitch();
        long now = System.currentTimeMillis();
        double yawVelocity = now > 0 ? deltaYaw / (now / 1000.0) : 0.0;
        double pitchVelocity = now > 0 ? deltaPitch / (now / 1000.0) : 0.0;
        yawVelocities.add(yawVelocity);
        pitchVelocities.add(pitchVelocity);
        if (yawVelocities.size() < 50) {
            return;
        }
        double correlation = correlation(yawVelocities.toArray(new Double[0]), pitchVelocities.toArray(new Double[0]));
        double acc = Math.abs(correlation);
        if (acc < 0.2 && isFastMovement()) {
            if (buffer++ > 5) {
                context.flagDetection(this, String.format("p=%.3f", correlation));
            }
        } else {
            buffer -= 0.6;
        }
        yawVelocities.poll();
        pitchVelocities.poll();
    }

    private double correlation(Double[] x, Double[] y) {
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;
        int n = Math.min(x.length, y.length);
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
        }
        double numerator = sumXY - (sumX * sumY / n);
        double denominator = Math.sqrt((sumX2 - sumX * sumX / n) * (sumY2 - sumY * sumY / n));
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private boolean isFastMovement() {
        double maxYaw = 0.0;
        for (Double yaw : yawVelocities) {
            maxYaw = Math.max(maxYaw, Math.abs(yaw));
        }
        return maxYaw > 3.0;
    }

    @Override
    public String getCheckName() {
        return "Cross Axis";
    }

    @Override
    public void reset() {
        yawVelocities.clear();
        pitchVelocities.clear();
    }
}
