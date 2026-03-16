package tech.zkmjnic.edgrim.checks.impl.movement;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.PositionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.PositionUpdate;

public class PredictionRunner extends Check implements PositionCheck {
    public PredictionRunner(EdGrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (!player.inVehicle()) {
            player.movementCheckRunner.processAndCheckMovementPacket(positionUpdate);
        }
    }
}
