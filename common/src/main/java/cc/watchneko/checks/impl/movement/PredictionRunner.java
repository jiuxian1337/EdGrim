package cc.watchneko.checks.impl.movement;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.type.PositionCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.PositionUpdate;

public class PredictionRunner extends Check implements PositionCheck {
    public PredictionRunner(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void onPositionUpdate(final PositionUpdate positionUpdate) {
        if (!player.inVehicle()) {
            player.movementCheckRunner.processAndCheckMovementPacket(positionUpdate);
        }
    }
}
