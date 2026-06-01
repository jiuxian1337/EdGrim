package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Simplification;
import tech.zkmjnic.edgrim.utils.math.Statistics;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AimHeuristicSmoothCheck implements HeuristicComponent {
    private final AimAA check;
    private final List<Double> stack = new ArrayList<>();
    private float lastAbsDeltaX, lastAbsDeltaY;
    private double lastRawAngle;

    public AimHeuristicSmoothCheck(final AimAA check) {
        this.check = check;
    }

    @Override
    public void process(final RotationUpdate event) {
        if (event.isCinematic2()) return;
        if (event.getDeltaXRotABS() == 0 && event.getDeltaYRotABS() == 0) return;

        final PlayerData player = check.getPlayer();
        final Vec2f delta = event.getDelta();
        final float absDeltaX = Math.abs(Math.abs(event.getTo().getYaw()) - Math.abs(event.getFrom().getYaw()));
        final float absDeltaY = Math.abs(Math.abs(event.getTo().getPitch()) - Math.abs(event.getFrom().getPitch()));
        double rawAngle = Euler.getAngleInDegrees(new Vec2f(delta.x(), delta.y()));
        double angle = rawAngle % 90;

        if ((absDeltaY > 1.5 && absDeltaX > 0.32) || absDeltaX > 1.5) {
            stack.add(angle);
            this.lastAbsDeltaX = absDeltaX;
            this.lastAbsDeltaY = absDeltaY;
            this.lastRawAngle = rawAngle;
        }
        if (stack.size() >= 20) {
            List<Float> jiff = Statistics.getJiffDelta(stack, 1);
            float prev = 999;
            float prePrev = 999;
            int zeroRunIdx = -1;
            for (int i = 0; i < jiff.size(); i++) {
                float f = jiff.get(i);
                if (f == 0.0 && prev == 0.0 && prePrev == 0) {
                    zeroRunIdx = i - 2;
                    break;
                }
                prePrev = prev;
                prev = f;
            }
            if (zeroRunIdx >= 0) {
                String stackSummary = stack.stream()
                        .map(d -> String.valueOf(Simplification.scaleVal(d, 2)))
                        .collect(Collectors.joining(","));
                if (check.flagAndAlert("* Invalid smoothing"
                        + "\nrawAngle=" + Simplification.scaleVal(lastRawAngle, 2)
                        + " dX=" + Simplification.scaleVal(lastAbsDeltaX, 2)
                        + " dY=" + Simplification.scaleVal(lastAbsDeltaY, 2)
                        + "\nzeroRun@" + zeroRunIdx + "/" + jiff.size()
                        + " stack=[" + stackSummary + "]")) {
                    check.getPlayer().mitigateDamage();
                }
            }
            stack.clear();
        }
    }
}

class Euler {
    public static double getAngleInDegrees(Vec2f delta) {
        double angleInRadians = Math.atan2(delta.x(), delta.y());
        double angleInDegrees = Math.toDegrees(angleInRadians);
        if (angleInDegrees < 0) {
            angleInDegrees += 360;
        }
        return angleInDegrees;
    }
}
