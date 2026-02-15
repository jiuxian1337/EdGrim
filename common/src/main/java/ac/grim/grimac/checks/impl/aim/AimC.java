package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

@CheckData(name = "AimC (Anomaly)", configName = "AimC", decay = 0.25, description = "Detects abnormal yaw or pitch sync patterns on non-player targets")
public final class AimC extends EdAimCheck {
    private double unnaturalBuffer;

    public AimC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (!hasAttackedSince(800L)) return;

        float deltaYaw = rotationUpdate.getProcessor().getDeltaYaw();
        float deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();
        float pitch = rotationUpdate.getProcessor().getPitch();
        float yaw = rotationUpdate.getProcessor().getYaw();
        float lastYaw = rotationUpdate.getProcessor().getLastYaw();

        if (player.getTarget() == null || player.getTarget().type == EntityTypes.PLAYER) return;

        if (player.getTarget() != player.getLastTarget()) {
            buffer = Math.max(0, buffer - getDecay());
            unnaturalBuffer = Math.max(0, unnaturalBuffer - getDecay());
            return;
        }

        double threshold = calculateSensitivity() > 130 ? 7 : 3;

        if (deltaYaw > threshold && deltaPitch < 0.01 && Math.abs(pitch) < 89) {
            buffer++;
            unnaturalBuffer = Math.max(0, unnaturalBuffer - getDecay());

            if (buffer > 3) {
                if (flagAndAlert(String.format("(Sync)\ndy= %.2f\ndp= %.2f\nthreshold= %.1f",
                        deltaYaw, deltaPitch, threshold))) {
                    mitigateDamage();
                }
            }

            if (deltaYaw > 170 && Math.abs(yaw - lastYaw) < 1) {
                if (flagAndAlert("(Change)\ndy= " + deltaYaw)) {
                    mitigateDamage();
                }
            }
        } else if (deltaYaw < 0.1 && deltaPitch > threshold) {
            unnaturalBuffer++;
            buffer = Math.max(0, buffer - getDecay());

            if (unnaturalBuffer > 3) {
                if (flagAndAlert("(Unnatural)\ndp= " + deltaPitch)) {
                    mitigateDamage();
                }
            }
        } else {
            buffer = Math.max(0, buffer - getDecay());
            unnaturalBuffer = Math.max(0, unnaturalBuffer - getDecay());
        }
    }
}
