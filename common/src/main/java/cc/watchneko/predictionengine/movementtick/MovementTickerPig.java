package cc.watchneko.predictionengine.movementtick;

import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.data.packetentity.PacketEntityRideable;
import cc.watchneko.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(PlayerData player) {
        super(player);
        movementInput = new Vector3dm(0, 0, 1);
    }

    @Override
    public float getSteeringSpeed() { // Vanilla multiples by 0.225f
        PacketEntityRideable pig = (PacketEntityRideable) player.compensatedEntities.self.getRiding();
        return (float) pig.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225f;
    }
}
