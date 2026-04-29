package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Statistics;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

import java.util.*;

public final class AimHeuristicSmoothCheck implements HeuristicComponent {
    private final AimAA check;
    private final List<Double> stack = new ArrayList<>();

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
        double angle = Euler.getAngleInDegrees(new Vec2f(delta.getX(), delta.getY())) % 90;

        if ((absDeltaY > 1.5 && absDeltaX > 0.32) || absDeltaX > 1.5) {
            stack.add(angle);
        }
        if (stack.size() >= 20) {
            List<Float> jiff = Statistics.getJiffDelta(stack, 1);
            float prev = 999;
            float prePrev = 999;
            for (float f : jiff) {
                if (f == 0.0 && prev == 0.0 && prePrev == 0) {
                    if (check.flagAndAlert("* Invalid smoothing")) {
                        check.getPlayer().mitigateDamage();
                    }
                    break;
                }
                prePrev = prev;
                prev = f;
            }
            stack.clear();
        }
    }
}

class Euler {
    public static double getAngleInDegrees(Vec2f delta) {
        double angleInRadians = Math.atan2(delta.getX(), delta.getY());
        double angleInDegrees = Math.toDegrees(angleInRadians);
        if (angleInDegrees < 0) {
            angleInDegrees += 360;
        }
        return angleInDegrees;
    }
}
