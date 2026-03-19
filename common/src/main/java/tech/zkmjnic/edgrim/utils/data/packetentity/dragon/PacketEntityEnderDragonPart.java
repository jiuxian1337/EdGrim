package tech.zkmjnic.edgrim.utils.data.packetentity.dragon;

import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public final class PacketEntityEnderDragonPart extends PacketEntity {
    public final DragonPart part;
    public final float width, height;

    public PacketEntityEnderDragonPart(PlayerData player, DragonPart part, double x, double y, double z, float width, float height) {
        super(player, null, EntityTypes.ENDER_DRAGON, x, y, z);
        this.part = part;
        this.width = width;
        this.height = height;
    }
}
