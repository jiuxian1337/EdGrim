package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import tech.zkmjnic.edgrim.utils.data.HeadRotation;

@CheckData(name = "AimN", configName = "AimN", description = "Detects overly consistent small rotations", decay = 0.55)
public final class AimN extends EdAimCheck {
    private float lastDeltaPitch;
    private float lastDeltaYaw;
    private int streak;

    public AimN(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        final HeadRotation to = rotationUpdate.getTo();
        final HeadRotation from = rotationUpdate.getFrom();
        float deltaPitch = Math.abs(to.getPitch() - from.getPitch());
        float deltaYaw = Math.abs(to.getYaw() - from.getYaw());

        if (isExempt(
                ExemptType.TELEPORT,
                ExemptType.SERVER_SENT_PULLBACK,
                ExemptType.SERVER_SENT_ROTATE,
                ExemptType.ELYTRA_FLYING,
                ExemptType.VEHICLE)) {
            return;
        }

        if (hasAttackedSince(250L)
                && (double) deltaYaw > 0.001
                && deltaYaw <= 5.0F
                && lastDeltaYaw <= 5.0F && Math.abs(to.getPitch()) <= 80.0F) {
            double gcdYAW = MathUtil.getGcd(deltaYaw, lastDeltaYaw);
            if (gcdYAW < 0.009 && !rotationUpdate.isCinematic()) {
                double gcdPITCH = MathUtil.getGcd(deltaPitch, lastDeltaPitch);
                if (deltaPitch > 0.0F && gcdPITCH < 0.009) {
                    streak = 0;
                    buffer = 0.0;
                }

                if (++streak > 20 && lastDeltaPitch == 0.0F && buffer++ > 20.0) {
                    if (flagAndAlert("gcdY= " + gcdYAW + "\ngcdP= " + gcdPITCH)) {
                        mitigateDamage();
                        buffer = 0.0;
                    }
                }
            } else {
                rewardBufferAndVL();
            }
        }

        lastDeltaPitch = deltaPitch;
        lastDeltaYaw = deltaYaw;
    }
}
