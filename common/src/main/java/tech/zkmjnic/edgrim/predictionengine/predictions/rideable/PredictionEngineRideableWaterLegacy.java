package tech.zkmjnic.edgrim.predictionengine.predictions.rideable;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineWaterLegacy;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableWaterLegacy extends PredictionEngineWaterLegacy {
    private final Vector3dm movementVector;

    @Override
    public void addJumpsToPossibilities(EdGrimPlayer player, Set<VectorData> existingVelocities) {
        PredictionEngineRideableUtils.handleJumps(player, existingVelocities);
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(EdGrimPlayer player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(movementVector, player, possibleVectors, speed);
    }
}
