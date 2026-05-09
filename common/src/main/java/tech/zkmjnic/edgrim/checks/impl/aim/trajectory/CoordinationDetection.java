package tech.zkmjnic.edgrim.checks.impl.aim.trajectory;

import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.time.Watch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

// Original author: Dg32z
// https://github.com/Dg32z
public class CoordinationDetection implements StatisticalDetectionStrategy {
    private final Queue<RotationSample> samples = new ArrayDeque<>(50);
    double buffer = 0;
    Watch checkTime = new Watch();

    @Override
    public void detect(PlayerData profile, DetectionContext context) {
        double deltaYaw = context.getDeltaYaw();
        double deltaPitch = context.getDeltaPitch();
        long timestamp = System.currentTimeMillis();
        samples.add(new RotationSample(deltaYaw, deltaPitch, timestamp));
        if (samples.size() < 50) return;
        if (!checkTime.hasTimeElapsed(300)) {
            checkTime.reset();
        }

        double[] yawAccels = calculateAccelerations(samples, true);
        double[] pitchAccels = calculateAccelerations(samples, false);
        double coordinationScore = calculateCoordinationScore(yawAccels, pitchAccels);
        double linearityScore = calculateLinearityDuringFastYaw();
        if (coordinationScore < -0.08
                && linearityScore > 220
                && MathUtil.getAverage(samples.stream().map(p -> p.deltaYaw).collect(Collectors.toList())) < 10
                && MathUtil.getAverage(samples.stream().map(p -> p.deltaPitch).collect(Collectors.toList())) < 5) {
            if (buffer++ > 30) {
                buffer -= 10;
                context.flagDetection(this, String.format("Invalid Coordination: rt=%.2f, li=%.2f", coordinationScore, linearityScore));
            }
        } else {
            buffer -= 1.4;
            if (buffer < 0) buffer = 0;
        }

        samples.poll();
    }

    private double[] calculateAccelerations(Queue<RotationSample> samples, boolean isYaw) {
        double[] accels = new double[30 - 2];
        RotationSample[] sampleArray = samples.toArray(new RotationSample[0]);
        for (int i = 2; i < 30; i++) {
            double vel1 = isYaw ? sampleArray[i - 1].deltaYaw : sampleArray[i - 1].deltaPitch;
            double vel0 = isYaw ? sampleArray[i - 2].deltaYaw : sampleArray[i - 2].deltaPitch;
            long time1 = sampleArray[i - 1].timestamp;
            long time0 = sampleArray[i - 2].timestamp;
            double dt = (time1 - time0) / 1000.0;
            if (dt == 0) dt = 0.001;
            accels[i - 2] = (vel1 - vel0) / dt;
        }
        return accels;
    }

    private double calculateCoordinationScore(double[] yawAccels, double[] pitchAccels) {
        return MathUtil.pearsonCorrelation(yawAccels, pitchAccels);
    }

    private double calculateLinearityDuringFastYaw() {
        List<Double> pitchMovements = new ArrayList<>();
        RotationSample[] sampleArray = samples.toArray(new RotationSample[0]);
        for (int i = 1; i < sampleArray.length; i++) {
            if (Math.abs(sampleArray[i].deltaYaw) > 5.0) {
                pitchMovements.add(sampleArray[i].deltaPitch);
            }
        }
        if (pitchMovements.size() < 5) return 0;
        return 1 - MathUtil.entropy(pitchMovements);
    }

    @Override
    public String getCheckName() {
        return "Coordination";
    }

    @Override
    public void changeTarget() {
    }

    @Override
    public void reset() {
        samples.clear();
    }

    private record RotationSample(double deltaYaw, double deltaPitch, long timestamp) {
    }
}
