package tech.zkmjnic.edgrim.checks.impl.interact;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockPlaceCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.nmsutil.InteractVisibilityUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "InteractBlock", description = "Interacting with blocks through occluding geometry")
public final class InteractBlock extends BlockPlaceCheck {

    public InteractBlock(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE || player.inVehicle()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, place.position)) {
            reward();
            return;
        }

        if (flagAndAlert("pos=" + place.position.getX() + "," + place.position.getY() + "," + place.position.getZ() + ", face=" + place.getFace())
                && shouldCancel() && shouldModifyPackets()) {
            place.resync();
        }
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE || player.inVehicle()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, place.position)) {
            reward();
            return;
        }

        flagAndAlert("post,pos=" + place.position.getX() + "," + place.position.getY() + "," + place.position.getZ() + ", face=" + place.getFace());
    }
}
