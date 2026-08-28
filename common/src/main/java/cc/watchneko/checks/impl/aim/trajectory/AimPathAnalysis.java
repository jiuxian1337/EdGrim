package cc.watchneko.checks.impl.aim.trajectory;

import cc.watchneko.player.PlayerData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class AimPathAnalysis implements StatisticalDetectionStrategy {
    private final Queue<Rotation> rotationSamples = new ArrayDeque<>();
    float buffer = 0;

    @Override
    public void detect(PlayerData profile, DetectionContext context) {
        rotationSamples.add(new Rotation(context.getDeltaYaw(), context.getDeltaPitch()));
        if (rotationSamples.size() < 20) return;

        double smoothness = calculateSmoothness();
        double linearity = calculateLinearity();
        if (linearity > 0.7 && smoothness < -0.2) {
            buffer++;
            if (buffer > 13) {
                buffer--;
                context.flagDetection(this, String.format("Linearity Rotation, s=%.2f, l=%.2f", smoothness, linearity));
            }
        } else {
            buffer -= 2f;
            if (buffer < 0) buffer = 0;
        }
        if (smoothness > 0.85 && linearity > 0.9) {
            context.flagDetection(this, String.format("Invalid Path, s=%.2f, l=%.2f", smoothness, linearity));
        }

        rotationSamples.poll();
    }

    private double calculateSmoothness() {
        List<Double> accelerations = new ArrayList<>();
        Rotation prev = null;
        Rotation prevPrev = null;
        for (Rotation current : rotationSamples) {
            if (prev != null && prevPrev != null) {
                double accelYaw = current.getYaw() - 2 * prev.getYaw() + prevPrev.getYaw();
                double accelPitch = current.getPitch() - 2 * prev.getPitch() + prevPrev.getPitch();
                accelerations.add(Math.sqrt(accelYaw * accelYaw + accelPitch * accelPitch));
            }
            prevPrev = prev;
            prev = current;
        }
        return 1 - (calculateStdDev(accelerations) / calculateMean(accelerations));
    }

    private double calculateLinearity() {
        List<Double> x = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        long startTime = rotationSamples.peek() != null ? rotationSamples.peek().getTimestamp() : 0;
        for (Rotation rot : rotationSamples) {
            x.add((double) (rot.getTimestamp() - startTime));
            y.add(Math.sqrt(rot.getYaw() * rot.getYaw() + rot.getPitch() * rot.getPitch()));
        }
        double n = x.size();
        double sumX = calculateMean(x) * n;
        double sumY = calculateMean(y) * n;
        double sumXY = 0;
        double sumX2 = 0;
        for (int i = 0; i < x.size(); i++) {
            sumXY += x.get(i) * y.get(i);
            sumX2 += x.get(i) * x.get(i);
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        double ssTot = 0;
        double ssRes = 0;
        double meanY = sumY / n;
        for (int i = 0; i < x.size(); i++) {
            double pred = slope * x.get(i) + intercept;
            ssTot += Math.pow(y.get(i) - meanY, 2);
            ssRes += Math.pow(y.get(i) - pred, 2);
        }
        return 1 - (ssRes / ssTot);
    }

    @Override
    public String getCheckName() {
        return "Aim Path Analysis";
    }

    @Override
    public void changeTarget() {
    }

    @Override
    public void reset() {
        rotationSamples.clear();
    }
}
