package tech.zkmjnic.edgrim.utils.data.packetentity;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

// We use simple interpolation here to be "close enough"
public class PacketEntityTrackXRot extends PacketEntity {
    public float packetYaw;
    public float interpYaw;
    public int steps = 0;

    public PacketEntityTrackXRot(EdGrimPlayer player, UUID uuid, EntityType type, double x, double y, double z, float xRot) {
        super(player, uuid, type, x, y, z);
        this.packetYaw = xRot;
        this.interpYaw = xRot;
    }

    @Override
    public void onMovement(boolean highBound) {
        super.onMovement(highBound);
        if (steps > 0) {
            interpYaw = interpYaw + ((packetYaw - interpYaw) / steps--);
        }
    }
}
