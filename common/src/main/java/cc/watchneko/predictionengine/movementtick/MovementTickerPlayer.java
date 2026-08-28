package cc.watchneko.predictionengine.movementtick;

import cc.watchneko.player.PlayerData;
import cc.watchneko.predictionengine.predictions.PredictionEngineLava;
import cc.watchneko.predictionengine.predictions.PredictionEngineNormal;
import cc.watchneko.predictionengine.predictions.PredictionEngineWater;
import cc.watchneko.predictionengine.predictions.PredictionEngineWaterLegacy;
import cc.watchneko.utils.nmsutil.BlockProperties;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class MovementTickerPlayer extends MovementTicker {
    public MovementTickerPlayer(PlayerData player) {
        super(player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            new PredictionEngineWater().guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction);
        } else {
            new PredictionEngineWaterLegacy().guessBestMovement(swimSpeed, player, swimFriction);
        }
    }

    @Override
    public void doLavaMove() {
        new PredictionEngineLava().guessBestMovement(0.02F, player);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        new PredictionEngineNormal().guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player);
    }
}
