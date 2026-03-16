package tech.zkmjnic.edgrim.checks.impl.movement;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.VehicleCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.PositionUpdate;
import tech.zkmjnic.edgrim.utils.anticheat.update.VehiclePositionUpdate;

public class VehiclePredictionRunner extends Check implements VehicleCheck {
    public VehiclePredictionRunner(EdGrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final VehiclePositionUpdate vehicleUpdate) {
        // Vehicle onGround = false always
        // We don't do vehicle setbacks because vehicle netcode sucks.
        player.movementCheckRunner.processAndCheckMovementPacket(new PositionUpdate(vehicleUpdate.getFrom(), vehicleUpdate.getTo(), false, null, null, vehicleUpdate.isTeleport()));
    }
}
