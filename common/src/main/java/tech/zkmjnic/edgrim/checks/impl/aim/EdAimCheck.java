package tech.zkmjnic.edgrim.checks.impl.aim;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.RotationCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

public abstract class EdAimCheck extends Check implements RotationCheck {
    protected double buffer;

    protected EdAimCheck(EdGrimPlayer player) {
        super(player);
    }

    protected long time() {
        return System.currentTimeMillis();
    }

    protected void rewardVL() {
        reward();
    }

    protected void rewardBufferAndVL() {
        if (buffer == 0.0) {
            rewardVL();
        } else {
            buffer = Math.max(0, buffer - getDecay());
        }
    }

    protected boolean isExempt(ExemptType... types) {
        for (ExemptType type : types) {
            if (isExempt(type)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isExempt(ExemptType type) {
        return switch (type) {
            case TELEPORT -> player.packetStateData.lastPacketWasTeleport;
            case SERVER_SENT_PULLBACK -> player.getSetbackTeleportUtil().isSendingSetback;
            case SERVER_SENT_ROTATE -> player.packetStateData.lastPacketWasTeleport;
            case ELYTRA_FLYING -> player.isGliding || player.isRiptidePose || player.packetStateData.tryingToRiptide;
            case VEHICLE -> player.inVehicle() || player.vehicleData.wasVehicleSwitch || player.packetStateData.receivedSteerVehicle;
            case VEHICLE_SWITCH -> player.vehicleData.wasVehicleSwitch;
            case RESPAWN -> !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport;
        };
    }

    protected boolean hasAttackedSince(long time) {
        return player.actionManager.hasAttackedSince(time);
    }

    protected int calculateSensitivity() {
        return player.calculateSensitivity();
    }

    protected boolean isMoving() {
        return player.isMoving();
    }

    protected void mitigateDamage() {
        player.mitigateDamage();
    }

    protected boolean isAboveSetbackVl() {
        return shouldSetback();
    }
}
