package cc.watchneko.checks.impl.movement;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.type.VehicleCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.PositionUpdate;
import cc.watchneko.utils.anticheat.update.VehiclePositionUpdate;

public class VehiclePredictionRunner extends Check implements VehicleCheck {
    public VehiclePredictionRunner(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void process(final VehiclePositionUpdate vehicleUpdate) {
        // Vehicle onGround = false always
        // We don't do vehicle setbacks because vehicle netcode sucks.
        player.movementCheckRunner.processAndCheckMovementPacket(new PositionUpdate(vehicleUpdate.getFrom(), vehicleUpdate.getTo(), false, null, null, vehicleUpdate.isTeleport()));
    }
}
