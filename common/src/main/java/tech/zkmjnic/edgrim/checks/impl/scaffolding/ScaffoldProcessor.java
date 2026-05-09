package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

public final class ScaffoldProcessor extends ScaffoldCheck {

    public ScaffoldProcessor(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            WrapperPlayClientPlayerBlockPlacement c08 = new WrapperPlayClientPlayerBlockPlacement(event);
            if (c08.getItemStack().get().getType().getPlacedType() != null && c08.getFace() != BlockFace.OTHER && shouldModifyPackets() && isCancelWindowActive()) {
                event.setCancelled(true);
            }
        }
    }
}
