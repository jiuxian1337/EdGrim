package tech.zkmjnic.edgrim.predictionengine.movementtick;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntityRideable;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(EdGrimPlayer player) {
        super(player);
        movementInput = new Vector3dm(0, 0, 1);
    }

    @Override
    public float getSteeringSpeed() { // Vanilla multiples by 0.225f
        PacketEntityRideable pig = (PacketEntityRideable) player.compensatedEntities.self.getRiding();
        return (float) pig.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225f;
    }
}
