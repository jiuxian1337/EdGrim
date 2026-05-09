package tech.zkmjnic.edgrim.predictionengine.movementtick;

import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.predictionengine.predictions.rideable.PredictionEngineHappyGhast;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntityHappyGhast;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

public class MovementTickerHappyGhast extends MovementTickerLivingVehicle {

    public MovementTickerHappyGhast(PlayerData player) {
        super(player);

        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        if (!happyGhastPacket.isControllingPassenger()) return;

        player.speed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;

        // Setup player inputs
        float sideways = player.vehicleData.vehicleHorizontal;
        float forward = 0.0F;
        float upAndDown = 0.0F;
        if (player.vehicleData.vehicleForward != 0.0F) {
            float xRot = player.yRot * 2F;
            float calcForward = player.trigHandler.cos(xRot * (float) (Math.PI / 180.0));
            float calcUpAndDown = -player.trigHandler.sin(xRot * (float) (Math.PI / 180.0));
            if (player.vehicleData.vehicleForward < 0.0F) {
                calcForward *= -0.5F;
                calcUpAndDown *= -0.5F;
            }

            upAndDown = calcUpAndDown;
            forward = calcForward;
        }

        if (player.lastJumping) {
            upAndDown += 0.5F;
        }

        this.movementInput = new Vector3dm(sideways, upAndDown, forward).multiply(3.9F * happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED));
    }

    @Override
    public void doNormalMove(float blockFriction) {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(movementInput, 0.91F).guessBestMovement(flyingSpeed, player);
    }

    @Override
    public void doLavaMove() {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(movementInput, 0.5).guessBestMovement(flyingSpeed, player);
    }

    @Override
    public void doWaterMove(float swimSpeed, boolean isFalling, float swimFriction) {
        PacketEntityHappyGhast happyGhastPacket = (PacketEntityHappyGhast) player.compensatedEntities.self.getRiding();
        float flyingSpeed = (float) happyGhastPacket.getAttributeValue(Attributes.FLYING_SPEED) * 5.0F / 3.0F;
        new PredictionEngineHappyGhast(movementInput, 0.8F).guessBestMovement(flyingSpeed, player);
    }

}
