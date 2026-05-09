package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

public final class ScaffoldProcessor extends ScaffoldCheck {

    public ScaffoldProcessor(PlayerData player) {
        super(player);
    }

//    @Override
//    public void onPacketReceive(PacketReceiveEvent event) {
//
//        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
//            WrapperPlayClientPlayerBlockPlacement c08 = new WrapperPlayClientPlayerBlockPlacement(event);
//            if (c08.getItemStack().get().getType().getPlacedType() != null && c08.getFace() != BlockFace.OTHER && isCancelWindowActive()) {
//                event.setCancelled(true);
//            }
//        }
//    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }

        final BlockFace face = place.getFace();
        if (face == BlockFace.OTHER) return;

        if (isCancelWindowActive()) {
            place.resync();
        }
    }
}
