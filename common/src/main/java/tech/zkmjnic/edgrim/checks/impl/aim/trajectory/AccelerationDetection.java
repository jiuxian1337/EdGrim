package tech.zkmjnic.edgrim.checks.impl.aim.trajectory;

import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class AccelerationDetection implements StatisticalDetectionStrategy {
    private final List<Double> yawAccels = new ArrayList<>();
    private final List<Double> pitchAccels = new ArrayList<>();
    private double buffer = 0;
    private double buffer2 = 0;
    private double buffer3 = 0;

    @Override
    public void detect(PlayerData profile, DetectionContext context) {
        double yawAccel = context.getYawAcceleration();
        double pitchAccel = context.getPitchAcceleration();
        double ignoreThreshold = 0.1;
        if (yawAccel < ignoreThreshold && pitchAccel < ignoreThreshold) return;

        yawAccels.add(yawAccel);
        pitchAccels.add(pitchAccel);
        if (yawAccels.size() < 40) return;

        double yawMean = calculateMean(yawAccels);
        double yawStdDev = calculateStdDev(yawAccels, yawMean);
        double pitchMean = calculateMean(pitchAccels);
        double pitchStdDev = calculateStdDev(pitchAccels, pitchMean);
        double yawZScore = Math.abs(yawAccel - yawMean) / yawStdDev;
        double pitchZScore = Math.abs(pitchAccel - pitchMean) / pitchStdDev;
        String debug = String.format("%.3f, %.3f, %.3f, %.3f", yawZScore, pitchZScore, yawMean, pitchMean);

        if (pitchZScore < 0.05 && yawZScore < 0.05 && Math.abs(yawMean) > 200 && Math.abs(pitchMean) > 200 && Math.abs(yawMean) < 800 && Math.abs(pitchMean) < 800) {
            buffer++;
            if (buffer >= 5) context.flagDetection(this, String.format("Pattern #1\n%s", debug));
        } else {
            buffer -= 0.4;
            if (buffer < 0) buffer = 0;
        }

        boolean largeMove = yawAccels.stream().anyMatch(a -> a > 10) || pitchAccels.stream().anyMatch(a -> a > 10);
        if (pitchZScore > 4 || yawZScore > 4 || !largeMove) {
            buffer2++;
            if (buffer2 >= 2) {
                // keep original empty branch
            }
        } else {
            buffer2 -= 0.1;
            if (buffer2 < 0) buffer2 = 0;
        }

        double scoreZ2Yaw = Math.abs(yawMean) * yawZScore;
        double scoreZ2Pitch = Math.abs(pitchMean) * pitchZScore;
        if (scoreZ2Yaw < 5 && scoreZ2Pitch < 5 && Math.abs(yawMean) > 2 && Math.abs(pitchMean) > 2) {
            buffer3++;
            if (buffer3 >= 4) {
                // keep original empty branch
            }
        } else {
            buffer3 -= 0.2;
            if (buffer3 < 0) buffer3 = 0;
        }

        if (yawAccels.size() > 40) {
            yawAccels.remove(0);
            pitchAccels.remove(0);
        }
    }

    private double calculateStdDev(List<Double> data, double mean) {
        double sum = 0;
        long count = 0;
        for (Double x : data) {
            sum += Math.pow(x - mean, 2);
            count++;
        }
        double variance = count > 0 ? sum / count : 0;
        return Math.sqrt(variance);
    }

    @Override
    public String getCheckName() {
        return "Acceleration";
    }

    @Override
    public void changeTarget() {
    }

    @Override
    public void reset() {
        yawAccels.clear();
        pitchAccels.clear();
    }
}
