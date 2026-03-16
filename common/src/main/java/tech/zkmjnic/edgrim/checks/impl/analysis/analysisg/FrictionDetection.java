package tech.zkmjnic.edgrim.checks.impl.analysis.analysisg;

import tech.zkmjnic.edgrim.checks.impl.analysis.AnalysisMathUtil;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FrictionDetection implements AimDetectionStrategy {
    private final Queue<Double> yawVelocities = new ArrayDeque<>(10);
    private final Queue<Double> pitchVelocities = new ArrayDeque<>(10);
    private final Queue<Double> yawDeltas = new ArrayDeque<>(20);
    private final Queue<Double> pitchDeltas = new ArrayDeque<>(20);
    private double buffer1;
    private double buffer2;
    private double buffer3;
    private double lastYawVelocity;
    private double lastPitchVelocity;
    private double lastYawAcceleration;
    private double lastPitchAcceleration;

    @Override
    public void detect(EdGrimPlayer player, DetectionContext context) {
        double currentYawVelocity = context.getPreviousYawVelocity();
        double currentPitchVelocity = context.getPreviousPitchVelocity();
        double currentYawAcceleration = context.getYawAcceleration();
        double currentPitchAcceleration = context.getPitchAcceleration();
        int sens = player.calculateSensitivity();
        yawVelocities.add(currentYawVelocity);
        pitchVelocities.add(currentPitchVelocity);
        if (yawVelocities.size() > 15) {
            yawVelocities.poll();
        } else {
            return;
        }
        if (pitchVelocities.size() > 15) {
            pitchVelocities.poll();
        } else {
            return;
        }

        double decelYaw = 0.0;
        double decelPitch = 0.0;
        boolean flagged = false;
        if (Math.abs(lastYawVelocity) > 30 && Math.abs(currentYawVelocity) < 5) {
            double expected = -lastYawVelocity;
            decelYaw = Math.abs(currentYawVelocity - expected);
            if (decelYaw > 200) {
                flagged = true;
            }
        } else if (Math.abs(lastPitchVelocity) > 30 && Math.abs(currentPitchVelocity) < 5) {
            double expected = -lastPitchVelocity;
            decelPitch = Math.abs(currentPitchVelocity - expected);
            if (decelPitch > 200) {
                flagged = true;
            }
        }
        if (flagged) {
            if (buffer1++ > 2) {
                buffer1 -= 1.0;
            }
        } else {
            buffer1 = Math.max(buffer1 - 0.02, 0.0);
        }

        if (Math.abs(currentYawVelocity) > 100 || Math.abs(currentPitchVelocity) > 100) {
            double correlation = AnalysisMathUtil.pearsonCorrelation(toFloat(yawVelocities), toFloat(pitchVelocities));
            if (Math.abs(correlation) < (sens > 80 ? -0.1 : -0.4)) {
                buffer2 += 1.2;
                if (buffer2 > 6) {
                    buffer2 -= 2;
                    context.flagDetection(this, String.format("patterned #1: %.2f, v2: %.2f, c: %.2f", currentYawVelocity, currentPitchVelocity, correlation));
                }
            } else {
                buffer2 = Math.max(0.0, buffer2 - 1.0);
            }
        } else {
            buffer2 = Math.max(0.0, buffer2 - 0.7);
        }

        double deltaY = Math.abs(currentYawAcceleration - lastYawAcceleration);
        double deltaX = Math.abs(currentPitchAcceleration - lastPitchAcceleration);
        if (deltaY > 1 && deltaX > 1) {
            yawDeltas.add(deltaY);
            pitchDeltas.add(deltaX);
        }
        if (yawDeltas.size() > 15) {
            yawDeltas.poll();
        }
        if (pitchDeltas.size() > 15) {
            pitchDeltas.poll();
        }
        if (yawDeltas.size() == 15 && pitchDeltas.size() == 15) {
            ArrayList<String> tags = new ArrayList<>();
            double limit3 = sens < 80 ? 3500 : 5000;
            if (deltaX > limit3 || deltaY > limit3) {
                buffer3 += 0.1;
                tags.add("large");
            }
            if (deltaX > 0 && deltaY > 0) {
                double ratioXY = deltaX / deltaY;
                double ratioYX = deltaY / deltaX;
                double limit = sens < 80 ? 80 : 65;
                if (ratioXY > limit || ratioYX > limit) {
                    buffer3 += 2.0;
                    tags.add("not harmonize, r=" + String.format("%.3f, %.3f", ratioXY, ratioYX));
                }
            }
            double frictionScore = frictionScore();
            double limit2 = sens < 80 ? -0.8 : -0.2;
            if (frictionScore < limit2) {
                buffer3 += 0.8;
                tags.add("low score, s=" + String.format("%.3f", frictionScore));
            }
            if (abruptChange(yawDeltas) || abruptChange(pitchDeltas)) {
                buffer3 += 0.2;
                tags.add("abrupt");
            }
            if (buffer3 >= 20) {
                buffer3 -= 4;
                context.flagDetection(this, String.format("combined friction dy: %.2f, dx: %.2f, tags=[%s]", deltaY, deltaX, String.join(",", tags)));
            }
        }
        buffer3 = Math.max(0.0, buffer3 - 1.0);
        lastYawVelocity = currentYawVelocity;
        lastPitchVelocity = currentPitchVelocity;
        lastYawAcceleration = currentYawAcceleration;
        lastPitchAcceleration = currentPitchAcceleration;
    }

    private List<Float> toFloat(Queue<Double> values) {
        ArrayList<Float> list = new ArrayList<>(values.size());
        for (double v : values) {
            list.add((float) v);
        }
        return list;
    }

    private double frictionScore() {
        double meanX = yawDeltas.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double meanY = pitchDeltas.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdX = stdDev(yawDeltas, meanX);
        double stdY = stdDev(pitchDeltas, meanY);
        double cvX = meanX != 0.0 ? stdX / meanX : 0.0;
        double cvY = meanY != 0.0 ? stdY / meanY : 0.0;
        return 1.0 - Math.max(cvX, cvY);
    }

    private boolean abruptChange(Queue<Double> history) {
        if (history.size() < 2) {
            return false;
        }
        Double[] deltas = history.toArray(new Double[0]);
        for (int i = 1; i < deltas.length; i++) {
            double change = Math.abs(deltas[i] - deltas[i - 1]);
            if (change > 3000.0) {
                return true;
            }
        }
        return false;
    }

    private double stdDev(Queue<Double> data, double mean) {
        if (data.isEmpty()) {
            return 0.0;
        }
        double variance = 0.0;
        for (double v : data) {
            variance += Math.pow(v - mean, 2);
        }
        return Math.sqrt(variance / data.size());
    }

    @Override
    public String getCheckName() {
        return "Friction";
    }

    @Override
    public void reset() {
        yawDeltas.clear();
        pitchDeltas.clear();
    }
}
