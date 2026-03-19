package tech.zkmjnic.edgrim.predictionengine.predictions;

import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWaterLegacy extends PredictionEngine {
    private float swimmingSpeed;
    private float swimmingFriction;

    public void guessBestMovement(float swimmingSpeed, PlayerData player, float swimmingFriction) {
        this.swimmingSpeed = swimmingSpeed;
        this.swimmingFriction = swimmingFriction;
        super.guessBestMovement(swimmingSpeed, player);
    }

    // This is just the vanilla equation for legacy water movement
    @Override
    public Vector3dm getMovementResultFromInput(PlayerData player, Vector3dm inputVector, float f, float f2) {
        float lengthSquared = (float) inputVector.lengthSquared();

        if (lengthSquared >= 1.0E-4F) {
            lengthSquared = (float) Math.sqrt(lengthSquared);

            if (lengthSquared < 1.0F) {
                lengthSquared = 1.0F;
            }

            lengthSquared = swimmingSpeed / lengthSquared;
            inputVector.multiply(lengthSquared);
            float sinResult = player.trigHandler.sin(player.xRot * 0.017453292F);
            float cosResult = player.trigHandler.cos(player.xRot * 0.017453292F);

            return new Vector3dm(inputVector.getX() * cosResult - inputVector.getZ() * sinResult,
                    inputVector.getY(), inputVector.getZ() * cosResult + inputVector.getX() * sinResult);
        }

        return new Vector3dm();
    }


    @Override
    public void addJumpsToPossibilities(PlayerData player, Set<VectorData> existingVelocities) {
        for (VectorData vector : new HashSet<>(existingVelocities)) {
            existingVelocities.add(new VectorData(vector.vector.clone().add(new Vector3dm(0, 0.04f, 0)), vector, VectorData.VectorType.Jump));

            if (player.skippedTickInActualMovement) {
                existingVelocities.add(new VectorData(vector.vector.clone().add(new Vector3dm(0, 0.02f, 0)), vector, VectorData.VectorType.Jump));
            }
        }
    }

    @Override
    public void endOfTick(PlayerData player, double playerGravity) {
        super.endOfTick(player, playerGravity);

        for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
            vector.vector.multiply(new Vector3dm(swimmingFriction, 0.8F, swimmingFriction));

            // Gravity
            vector.vector.setY(vector.vector.getY() - 0.02D);
        }
    }
}
