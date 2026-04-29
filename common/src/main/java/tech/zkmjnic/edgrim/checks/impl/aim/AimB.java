package tech.zkmjnic.edgrim.checks.impl.aim;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(
        name = "AimB",
        configName = "AimB",
        description = "rotate like aimassist",
        decay = 0.85,
        experimental = true
)
public final class AimB extends Check implements RotationCheck {
    private double buffer2;
    private double buffer3;
    private float lastDeltaYaw;

    public AimB(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        if (player.packetStateData.lastPacketWasTeleport
                || player.vehicleData.wasVehicleSwitch
                || player.packetStateData.horseInteractCausedForcedRotation
                || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate
                || player.predictedVelocity.isKnockback()
                || player.compensatedEntities.self.getRiding() != null) {
            return;
        }

        if (Math.abs(rotationUpdate.getTo().getPitch()) == 90 || player.getTarget() == null || player.getLastTarget() == null) {
            return;
        }

        double distanceNow = player.getTarget().getPossibleCollisionBoxes().distance(player.getBoundingBox());
        double distancePrev = player.getLastTarget().getPossibleCollisionBoxes().distance(player.getBoundingBox());
        if (distancePrev < distanceNow || player.getTarget().type != EntityTypes.PLAYER) {
            return;
        }

        float deltaPitch = Math.abs(player.getPitch() - player.getLastPitch());
        float deltaYaw = Math.abs(player.getYaw() - player.getLastYaw());
        float lastDeltaPitch = rotationUpdate.getProcessor().getLastDeltaPitch();
        float pitchDifference = Math.abs(lastDeltaPitch - deltaPitch);
        float yawDifference = Math.abs(lastDeltaYaw - deltaYaw);

        if (deltaYaw > yawDifference
                && yawDifference > 0.3
                && deltaPitch > 0
                && player.calculateSensitivity() > 48
                && deltaPitch <= pitchDifference
                && pitchDifference < 0.1
                && player.actionManager.hasAttackedSince(500L)) {
            if (buffer++ > 7.5 && flagAndAlert("(InvalidMouse)\ndiffx= " + deltaYaw + "\ndiffy= " + deltaPitch + "\ndp= " + deltaPitch)) {
                player.mitigateDamage();
            } else {
                rewardBufferAndVL();
            }
        }

        if ((deltaPitch > 1.5f || deltaYaw > 3.0f)
                && !rotationUpdate.isCinematic2()
                && (player.getPitch() == 0 || player.getPitch() % 0.01f == 0)
                && player.actionManager.hasAttackedSince(600L)
                && player.calculateSensitivity() > 50) {
            if (buffer2++ > 3) {
                if (flagAndAlert("(Randomizer-Flaw)\ndp= " + deltaPitch + "\ndy= " + deltaYaw) && isAboveSetbackVl()) {
                    player.mitigateDamage();
                }
            } else {
                buffer2 = Math.max(buffer2 - getDecay(), 0);
            }
        }

        if (deltaYaw > yawDifference
                && yawDifference > 0.0
                && deltaPitch > 0
                && deltaPitch < 0.02
                && pitchDifference > deltaPitch * 2
                && !rotationUpdate.isCinematic2()
                && player.actionManager.hasAttackedSince(400L)) {
            if (buffer3++ > 5) {
                if (flagAndAlert("(Radomization-Extremerly)\ndiffx= " + deltaYaw + "\ndiffy= " + deltaPitch + "\ndy= " + deltaYaw)) {
                    buffer3 = 0;
                }
            } else {
                buffer3 = Math.max(buffer3 - getDecay(), 0);
            }
        }
        lastDeltaYaw = deltaYaw;
    }
}
