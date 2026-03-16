package tech.zkmjnic.edgrim.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimG", configName = "AimG", decay = 0.86, setback = 4, description = "Detects smooth pitch steps with unusually low acceleration")
public final class AimG extends EdAimCheck {
    int maxBuffer;
    double minDeltaY;
    double maxDeltaYAccel;
    double lastDeltaY;

    public AimG(EdGrimPlayer player) {
        super(player);
        lastDeltaY = 0;
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        if (hasAttackedSince(150L)) {

            double deltaYAccel = rotationUpdate.getProcessor().getPitchAccel();
            double deltaPitch = rotationUpdate.getProcessor().getDeltaPitch();

            if (!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
                return;
            }

            if (isExempt(
                    ExemptType.TELEPORT,
                    ExemptType.SERVER_SENT_PULLBACK,
                    ExemptType.SERVER_SENT_ROTATE,
                    ExemptType.ELYTRA_FLYING,
                    ExemptType.VEHICLE)) {
                return;
            }

            if (rotationUpdate.isCinematic()) {
                return;
            }

            if (deltaYAccel < maxDeltaYAccel && deltaPitch > minDeltaY) {
                if (buffer++ > maxBuffer) {
                    if (flagAndAlert("dya= " + deltaYAccel
                            + "\ndy= " + deltaPitch)) {
                        buffer = 0;
                        if (isAboveSetbackVl()) {
                            mitigateDamage();
                        }
                    }
                }
            }
        } else {
            rewardBufferAndVL();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getIntElse(getConfigName() + ".buffer", 7);
        minDeltaY = config.getDoubleElse(getConfigName() + ".min-deltaY", 0.4D);
        maxDeltaYAccel = config.getDoubleElse(getConfigName() + ".max-deltaY-accel", 0.1D);
    }
}
