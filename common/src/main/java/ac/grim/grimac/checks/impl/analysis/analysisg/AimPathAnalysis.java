package ac.grim.grimac.checks.impl.analysis.analysisg;

import ac.grim.grimac.player.GrimPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class AimPathAnalysis implements StatisticalDetectionStrategy {
    private final Queue<Rotation> rotationSamples = new ArrayDeque<>();
    private float buffer;

    @Override
    public void detect(GrimPlayer player, DetectionContext context) {
        rotationSamples.add(new Rotation(context.getDeltaYaw(), context.getDeltaPitch()));
        if (rotationSamples.size() < 20) {
            return;
        }
        double smoothness = smoothness();
        double linearity = linearity();
        if (linearity > 0.7 && smoothness < -0.2) {
            buffer += 1.0f;
            if (buffer > 13) {
                buffer -= 1.0f;
                context.flagDetection(this, String.format("Linearity Rotation, s=%.2f, l=%.2f", smoothness, linearity));
            }
        } else {
            buffer -= 2.0f;
            if (buffer < 0) {
                buffer = 0;
            }
        }
        if (smoothness > 0.85 && linearity > 0.9) {
            context.flagDetection(this, String.format("Invalid Path, s=%.2f, l=%.2f", smoothness, linearity));
        }
        rotationSamples.poll();
    }

    private double smoothness() {
        List<Double> accelerations = new ArrayList<>();
        Rotation prev = null;
        Rotation prevPrev = null;
        for (Rotation cur : rotationSamples) {
            if (prev != null && prevPrev != null) {
                double accelYaw = cur.getYaw() - 2 * prev.getYaw() + prevPrev.getYaw();
                double accelPitch = cur.getPitch() - 2 * prev.getPitch() + prevPrev.getPitch();
                accelerations.add(Math.sqrt(accelYaw * accelYaw + accelPitch * accelPitch));
            }
            prevPrev = prev;
            prev = cur;
        }
        double mean = mean(accelerations);
        double std = stdDev(accelerations, mean);
        return mean == 0.0 ? 0.0 : 1.0 - (std / mean);
    }

    private double linearity() {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        long start = rotationSamples.peek() == null ? 0 : rotationSamples.peek().getTimestamp();
        for (Rotation rot : rotationSamples) {
            x.add((double) (rot.getTimestamp() - start));
            y.add(Math.sqrt(rot.getYaw() * rot.getYaw() + rot.getPitch() * rot.getPitch()));
        }
        double n = x.size();
        if (n == 0) {
            return 0.0;
        }
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < x.size(); i++) {
            sumX += x.get(i);
            sumY += y.get(i);
            sumXY += x.get(i) * y.get(i);
            sumX2 += x.get(i) * x.get(i);
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0.0) {
            return 0.0;
        }
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        double ssTot = 0.0;
        double ssRes = 0.0;
        double meanY = sumY / n;
        for (int i = 0; i < x.size(); i++) {
            double pred = slope * x.get(i) + intercept;
            ssTot += Math.pow(y.get(i) - meanY, 2);
            ssRes += Math.pow(y.get(i) - pred, 2);
        }
        return ssTot == 0.0 ? 0.0 : 1.0 - (ssRes / ssTot);
    }

    private double mean(List<Double> data) {
        return data.isEmpty() ? 0.0 : data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double stdDev(List<Double> data, double mean) {
        if (data.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : data) {
            double diff = v - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / data.size());
    }

    @Override
    public String getCheckName() {
        return "Aim Path Analysis";
    }

    @Override
    public void reset() {
        rotationSamples.clear();
    }
}
