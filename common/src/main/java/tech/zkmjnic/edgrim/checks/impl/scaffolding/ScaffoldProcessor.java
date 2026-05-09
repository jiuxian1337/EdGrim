package tech.zkmjnic.edgrim.checks.impl.scaffolding;

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
//            if (c08.getItemStack().get().getType().getPlacedType() != null && c08.getFace() != BlockFace.OTHER && canCancel()) {
//                event.setCancelled(true);
//            }
//        }
//    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }
        if (cancelPlacementsUntil == 0L) {
            return;
        }
        if (System.currentTimeMillis() >= cancelPlacementsUntil) {
            cancelPlacementsUntil = 0L;
            return;
        }

        place.resync();

    }
}
