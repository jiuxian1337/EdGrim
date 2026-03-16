package tech.zkmjnic.edgrim.predictionengine.movementtick;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntityHorse;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import tech.zkmjnic.edgrim.utils.nmsutil.Collisions;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class MovementTickerHorse extends MovementTickerLivingVehicle {

    public MovementTickerHorse(EdGrimPlayer player) {
        super(player);

        PacketEntityHorse horsePacket = (PacketEntityHorse) player.compensatedEntities.self.getRiding();

        if (!horsePacket.hasSaddle()) return;

        player.speed = horsePacket.getAttributeValue(Attributes.MOVEMENT_SPEED) + getExtraSpeed();

        // Setup player inputs
        float horizInput = player.vehicleData.vehicleHorizontal * 0.5F;
        float forwardsInput = player.vehicleData.vehicleForward;

        if (forwardsInput <= 0.0F) {
            forwardsInput *= 0.25F;
        }

        this.movementInput = new Vector3dm(horizInput, 0, forwardsInput);
        if (movementInput.lengthSquared() > 1) movementInput.normalize();
    }

    @Override
    public void livingEntityAIStep() {
        super.livingEntityAIStep();
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17))
            Collisions.handleInsideBlocks(player);
    }

    public float getExtraSpeed() {
        return 0f;
    }
}
