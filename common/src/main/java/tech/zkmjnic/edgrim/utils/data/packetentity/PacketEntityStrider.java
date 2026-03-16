package tech.zkmjnic.edgrim.utils.data.packetentity;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityStrider extends PacketEntityRideable {
    public boolean isShaking = false;

    public PacketEntityStrider(EdGrimPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }
}
