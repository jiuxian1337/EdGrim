package tech.zkmjnic.edgrim.utils.data.packetentity;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.UUID;

public class PacketEntityUnHittable extends PacketEntity {

    public PacketEntityUnHittable(PlayerData player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public boolean canHit() {
        return false;
    }
}
