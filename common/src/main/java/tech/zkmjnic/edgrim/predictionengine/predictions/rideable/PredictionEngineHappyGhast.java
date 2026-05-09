package tech.zkmjnic.edgrim.predictionengine.predictions.rideable;

import lombok.RequiredArgsConstructor;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.predictionengine.predictions.PredictionEngineNormal;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class PredictionEngineHappyGhast extends PredictionEngineNormal {
    private final Vector3dm movementVector;
    private final double multiplier;

    @Override
    public void endOfTick(PlayerData player, double delta) {
        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.setX(vector.vector.getX() * multiplier);
            vector.vector.setY(vector.vector.getY() * multiplier);
            vector.vector.setZ(vector.vector.getZ() * multiplier);
        }
    }

    @Override
    public List<VectorData> applyInputsToVelocityPossibilities(PlayerData player, Set<VectorData> possibleVectors, float speed) {
        return PredictionEngineRideableUtils.applyInputsToVelocityPossibilities(this, movementVector, player, possibleVectors, speed);
    }

    @Override
    public Vector3dm getMovementResultFromInput(PlayerData player, Vector3dm inputVector, float flyingSpeed, float yRot) {
        float sin = player.trigHandler.sin(yRot * 0.017453292f);
        float cos = player.trigHandler.cos(yRot * 0.017453292f);

        double xResult = inputVector.getX() * cos - inputVector.getZ() * sin;
        double zResult = inputVector.getZ() * cos + inputVector.getX() * sin;

        return new Vector3dm(xResult * flyingSpeed, inputVector.getY() * flyingSpeed, zResult * flyingSpeed);
    }

}
