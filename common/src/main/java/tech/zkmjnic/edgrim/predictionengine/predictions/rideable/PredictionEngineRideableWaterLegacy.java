package tech.zkmjnic.edgrim.predictionengine.predictions.rideable;

import lombok.RequiredArgsConstructor;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineWaterLegacy;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableWaterLegacy extends PredictionEngineWaterLegacy {
    private final Vector3dm movementVector;

    @Override
    public void addJumpsToPossibilities(PlayerData player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(PlayerData player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }
}
