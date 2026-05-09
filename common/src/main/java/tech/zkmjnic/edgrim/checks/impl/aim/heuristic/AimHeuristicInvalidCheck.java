package tech.zkmjnic.edgrim.checks.impl.aim.heuristic;

import tech.zkmjnic.edgrim.checks.impl.aim.AimAA;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.Statistics;
import tech.zkmjnic.edgrim.utils.math.Vec2f;

public final class AimHeuristicInvalidCheck implements HeuristicComponent {
    private static final float INVALID_PITCH = 90f + 1e-6f;
    private static final int BUFFER_LIMIT = 70;
    private static final int ADD_BUFFER = 20;
    private final AimAA check;
    private int buffer;

    public AimHeuristicInvalidCheck(final AimAA check) {
        this.check = check;
    }

    @Override
    public void process(final RotationUpdate event) {
        if (event.getDeltaXRotABS() == 0 && event.getDeltaYRotABS() == 0) return;
        final PlayerData player = check.getPlayer();
        final Vec2f delta = event.getDelta();
        final float absDeltaX = Math.abs(Math.abs(event.getTo().getYaw()) - Math.abs(event.getFrom().getYaw()));
        final float absDeltaY = Math.abs(Math.abs(event.getTo().getPitch()) - Math.abs(event.getFrom().getPitch()));

        if (Statistics.isExponentiallySmall(absDeltaY)
                && absDeltaY > 0.0
                && absDeltaX > 0.5f) {
            buffer += ADD_BUFFER;
            if (buffer > BUFFER_LIMIT) {
                if (check.flagAndAlert("* Invalid Pitch " + delta.y())) {
                    check.getPlayer().mitigateDamage();
                }
                buffer = BUFFER_LIMIT - 1;
            }
        } else {
            buffer = Math.max(0, buffer - 1);
        }

        if (event.getTo().getPitch() > INVALID_PITCH) {
            if (check.flagAndAlert("* Unlimited Pitch " + delta.y())) {
                check.getPlayer().mitigateDamage();
            }
        }
    }
}
