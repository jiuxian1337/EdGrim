package tech.zkmjnic.edgrim.predictionengine.movementtick;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.predictionengine.predictions.rideable.PredictionEngineRideableLava;
import tech.zkmjnic.edgrim.predictionengine.predictions.rideable.PredictionEngineRideableNormal;
import tech.zkmjnic.edgrim.predictionengine.predictions.rideable.PredictionEngineRideableWater;
import tech.zkmjnic.edgrim.predictionengine.predictions.rideable.PredictionEngineRideableWaterLegacy;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import tech.zkmjnic.edgrim.utils.nmsutil.BlockProperties;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class MovementTickerLivingVehicle extends MovementTicker {
    Vector3dm movementInput = new Vector3dm();

    public MovementTickerLivingVehicle(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            new PredictionEngineRideableWater(movementInput).guessBestMovement(swimSpeed, player, isFalling, player.gravity, swimFriction);
        } else {
            new PredictionEngineRideableWaterLegacy(movementInput).guessBestMovement(swimSpeed, player, swimFriction);
        }
    }

    @Override
    public void doLavaMove() {
        new PredictionEngineRideableLava(movementInput).guessBestMovement(0.02F, player);
    }

    @Override
    public void doNormalMove(float blockFriction) {
        new PredictionEngineRideableNormal(movementInput).guessBestMovement(BlockProperties.getFrictionInfluencedSpeed(blockFriction, player), player);
    }
}
