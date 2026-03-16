package tech.zkmjnic.edgrim.checks.impl.analysis.analysisg;

import tech.zkmjnic.edgrim.checks.impl.analysis.AnalysisMathUtil;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CoordinationDetection implements StatisticalDetectionStrategy {
    private final Queue<RotationSample> samples = new ArrayDeque<>(50);
    private double buffer;
    private long lastCheck;

    @Override
    public void detect(EdGrimPlayer player, DetectionContext context) {
        double deltaYaw = context.getDeltaYaw();
        double deltaPitch = context.getDeltaPitch();
        long timestamp = System.currentTimeMillis();
        samples.add(new RotationSample(deltaYaw, deltaPitch, timestamp));
        if (samples.size() < 50) {
            return;
        }
        if (System.currentTimeMillis() - lastCheck < 300) {
            lastCheck = System.currentTimeMillis();
        }
        double[] yawAccels = accelerations(samples, true);
        double[] pitchAccels = accelerations(samples, false);
        double coordinationScore = AnalysisMathUtil.pearsonCorrelation(toFloatList(yawAccels), toFloatList(pitchAccels));
        double linearityScore = linearityDuringFastYaw();
        if (coordinationScore < -0.08 && linearityScore > 220 && averageYaw() < 10 && averagePitch() < 5) {
            if (buffer++ > 30) {
                buffer -= 10;
                context.flagDetection(this, String.format("Invalid Coordination: rt=%.2f, li=%.2f", coordinationScore, linearityScore));
            }
        } else {
            buffer -= 1.4;
            if (buffer < 0) {
                buffer = 0;
            }
        }
        samples.poll();
    }

    private double[] accelerations(Queue<RotationSample> sampleQueue, boolean yaw) {
        RotationSample[] arr = sampleQueue.toArray(new RotationSample[0]);
        double[] accels = new double[28];
        for (int i = 2; i < 30; i++) {
            double v1 = yaw ? arr[i - 1].deltaYaw : arr[i - 1].deltaPitch;
            double v0 = yaw ? arr[i - 2].deltaYaw : arr[i - 2].deltaPitch;
            long t1 = arr[i - 1].timestamp;
            long t0 = arr[i - 2].timestamp;
            double dt = (t1 - t0) / 1000.0;
            if (dt == 0) {
                dt = 0.001;
            }
            accels[i - 2] = (v1 - v0) / dt;
        }
        return accels;
    }

    private double linearityDuringFastYaw() {
        List<Double> pitchMoves = new ArrayList<>();
        RotationSample[] sampleArray = samples.toArray(new RotationSample[0]);
        for (int i = 1; i < sampleArray.length; i++) {
            if (Math.abs(sampleArray[i].deltaYaw) > 5.0) {
                pitchMoves.add(sampleArray[i].deltaPitch);
            }
        }
        if (pitchMoves.size() < 5) {
            return 0.0;
        }
        return 1.0 - AnalysisMathUtil.entropy(pitchMoves);
    }

    private double averageYaw() {
        double sum = 0.0;
        for (RotationSample s : samples) {
            sum += s.deltaYaw;
        }
        return sum / samples.size();
    }

    private double averagePitch() {
        double sum = 0.0;
        for (RotationSample s : samples) {
            sum += s.deltaPitch;
        }
        return sum / samples.size();
    }

    private List<Float> toFloatList(double[] values) {
        List<Float> list = new ArrayList<>(values.length);
        for (double v : values) {
            list.add((float) v);
        }
        return list;
    }

    @Override
    public String getCheckName() {
        return "Coordination";
    }

    @Override
    public void reset() {
        samples.clear();
    }

    private record RotationSample(double deltaYaw, double deltaPitch, long timestamp) {
    }
}
