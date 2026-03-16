package tech.zkmjnic.edgrim.utils.data.packetentity;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityArmorStand extends PacketEntity {

    public boolean isMarker = false;

    public PacketEntityArmorStand(EdGrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, int extraData) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public boolean canHit() {
        return !isMarker && super.canHit();
    }
}
