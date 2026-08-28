package cc.watchneko.checks.impl.aim.heuristic;

import cc.watchneko.checks.impl.aim.AimAA;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.lists.EvictingList;
import cc.watchneko.utils.math.Simplification;
import cc.watchneko.utils.math.Statistics;

import java.util.List;

public final class AimHeuristicFactorCheck implements HeuristicComponent {
    private static final float BUFFER_LIMIT = 3.0f;
    private static final int TICKS_TO_RESET = 2500;
    private final AimAA check;
    private final List<Double> stack = new EvictingList<>(3);
    private boolean lastIsNoRotation = false;
    private double lastHash;
    private float buffer;
    private int ticksToReset;

    public AimHeuristicFactorCheck(final AimAA check) {
        this.check = check;
    }

    @Override
    public void process(final RotationUpdate event) {
        boolean isNoRotation = event.getDeltaXRotABS() == 0 && event.getDeltaYRotABS() == 0;
        if (isNoRotation) {
            if (!lastIsNoRotation) stack.add(0.0);
            check();
            lastIsNoRotation = true;
        } else {
            float deltaYaw = Math.abs(Math.abs(event.getTo().getYaw()) - Math.abs(event.getFrom().getYaw()));
            stack.add(Simplification.scaleVal(deltaYaw, 2));
            check();
            lastIsNoRotation = false;
        }
    }

    private void check() {
        if (stack.size() != 3) return;
        double hash = stack.get(0) + stack.get(1) + stack.get(2);
        if (hash == lastHash) return;
        double centre = stack.get(1);
        boolean hugeRotation = centre > 35;

        if (hugeRotation && centre != 360.0f) {
            double compare = 1.2;
            boolean invalid = (stack.get(0) < compare && stack.get(2) < compare)
                    || (stack.get(0) > 55 && stack.get(1) < 2 && stack.get(2) > 55)
                    || Statistics.getMax(stack) > 70 && Statistics.getMin(stack) < compare && Statistics.getDistinct(stack) != 3;

            if (invalid) {
                float localVl = (centre > 160) ? 3 : (centre < 60) ? 1 : 2;
                buffer += localVl;
                if (buffer >= BUFFER_LIMIT) {
                    if (check.flagAndAlert("* Factor analysis (" + centre + "/"
                            + Simplification.scaleVal(stack.get(0) + stack.get(2), 2) + ")")) {
                        check.getPlayer().mitigateDamage();
                    }
                    buffer = BUFFER_LIMIT - 1;
                }
            }
        } else {
            ticksToReset++;
            if (ticksToReset >= TICKS_TO_RESET) {
                ticksToReset = 0;
                buffer = 0;
            }
        }
        lastHash = hash;
    }
}
