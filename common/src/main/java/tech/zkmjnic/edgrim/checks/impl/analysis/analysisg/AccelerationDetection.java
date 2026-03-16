package tech.zkmjnic.edgrim.checks.impl.analysis.analysisg;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;

import java.util.ArrayList;
import java.util.List;

public class AccelerationDetection implements StatisticalDetectionStrategy {
    private final List<Double> yawAccels = new ArrayList<>();
    private final List<Double> pitchAccels = new ArrayList<>();
    private double buffer;
    private double buffer2;
    private double buffer3;

    @Override
    public void detect(EdGrimPlayer player, DetectionContext context) {
        double yawAccel = context.getYawAcceleration();
        double pitchAccel = context.getPitchAcceleration();
        if (yawAccel < 0.1 && pitchAccel < 0.1) {
            return;
        }
        yawAccels.add(yawAccel);
        pitchAccels.add(pitchAccel);
        if (yawAccels.size() < 40) {
            return;
        }
        double yawMean = mean(yawAccels);
        double yawStd = stdDev(yawAccels, yawMean);
        double pitchMean = mean(pitchAccels);
        double pitchStd = stdDev(pitchAccels, pitchMean);
        double yawZ = yawStd == 0.0 ? 0.0 : Math.abs(yawAccel - yawMean) / yawStd;
        double pitchZ = pitchStd == 0.0 ? 0.0 : Math.abs(pitchAccel - pitchMean) / pitchStd;

        if (pitchZ < 0.05 && yawZ < 0.05 && Math.abs(yawMean) > 200 && Math.abs(pitchMean) > 200
                && Math.abs(yawMean) < 800 && Math.abs(pitchMean) < 800) {
            buffer += 1.0;
            if (buffer >= 5.0) {
                context.flagDetection(this, String.format("Pattern #1 y=%.3f p=%.3f", yawZ, pitchZ));
            }
        } else {
            buffer = Math.max(0.0, buffer - 0.4);
        }

        boolean largeMove = yawAccels.stream().anyMatch(a -> a > 10.0) || pitchAccels.stream().anyMatch(a -> a > 10.0);
        if (pitchZ > 4.0 || yawZ > 4.0 || !largeMove) {
            buffer2 += 1.0;
        } else {
            buffer2 = Math.max(0.0, buffer2 - 0.1);
        }

        double scoreZ2Yaw = Math.abs(yawMean) * yawZ;
        double scoreZ2Pitch = Math.abs(pitchMean) * pitchZ;
        if (scoreZ2Yaw < 5 && scoreZ2Pitch < 5 && Math.abs(yawMean) > 2 && Math.abs(pitchMean) > 2) {
            buffer3 += 1.0;
        } else {
            buffer3 = Math.max(0.0, buffer3 - 0.2);
        }

        if (yawAccels.size() > 40) {
            yawAccels.remove(0);
            pitchAccels.remove(0);
        }
    }

    private double mean(List<Double> data) {
        double sum = 0.0;
        for (double v : data) {
            sum += v;
        }
        return data.isEmpty() ? 0.0 : sum / data.size();
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
        return "Acceleration";
    }

    @Override
    public void reset() {
        yawAccels.clear();
        pitchAccels.clear();
    }
}
