package tech.zkmjnic.edgrim.predictionengine.movementtick;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineLava;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineNormal;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineWater;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineWaterLegacy;
import tech.zkmjnic.edgrim.utils.nmsutil.BlockProperties;

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
