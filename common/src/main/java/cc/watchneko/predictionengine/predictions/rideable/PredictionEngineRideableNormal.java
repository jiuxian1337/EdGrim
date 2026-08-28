package cc.watchneko.predictionengine.predictions.rideable;

import cc.watchneko.player.PlayerData;
import cc.watchneko.predictionengine.predictions.PredictionEngineNormal;
import cc.watchneko.utils.data.VectorData;
import cc.watchneko.utils.math.Vector3dm;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineRideableNormal extends PredictionEngineNormal {
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
