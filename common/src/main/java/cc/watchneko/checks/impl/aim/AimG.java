package cc.watchneko.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.RotationCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.math.MathUtil;

@CheckData(
        name = "AimG",
        description = "descending micro-pitch smoothing"
)
public final class AimG extends Check implements RotationCheck {
    private double streak;
    private double vl;
    private float lastDeltaPitch;
    private int maxStreak = 12;
    private int vlForStreak = 2;

    public AimG(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate update) {
        final float deltaPitch = Math.abs(MathUtil.getAngleDifference(update.getFrom().getPitch(), update.getTo().getPitch()));
        final float deltaYaw = Math.abs(MathUtil.getAngleDifference(update.getFrom().getYaw(), update.getTo().getYaw()));
        final double pitchAcceleration = Math.abs(lastDeltaPitch - deltaPitch);
        final boolean preemptive = deltaYaw > 1.975f && !update.isCinematic() && player.actionManager.hasAttackedSince(2000L);

        if (preemptive) {
            final boolean invalid = deltaPitch < lastDeltaPitch && deltaPitch < 0.0700001F && deltaPitch > 0.0015f;
            if (invalid) {
                if (streak++ > maxStreak) {
                    vl++;
                } else {
                    vl = Math.max(0, vl - 0.25);
                }
                if (vl > vlForStreak && flagAndAlert("%yC=" + deltaYaw + " %pC=" + deltaPitch + " %ascension=" + pitchAcceleration + " %sup=" + (deltaPitch + pitchAcceleration))) {
                    streak = 0;
                    player.mitigateDamage();
                }
            } else {
                streak = Math.max(0, streak - 0.5);
                vl = Math.max(0, vl - 0.25);
            }
        }

        lastDeltaPitch = deltaPitch;
    }

    @Override
    public void onReload(ConfigManager config) {
        maxStreak = config.getIntElse("AimG.max-streak", maxStreak);
        vlForStreak = config.getIntElse("AimG.vl-for-streak", vlForStreak);
    }
}
