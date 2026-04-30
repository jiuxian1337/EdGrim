package tech.zkmjnic.edgrim.checks.impl.interact;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockPlaceCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.nmsutil.InteractVisibilityUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "InteractA", description = "block interaction visibility check", decay = 0.05)
public final class InteractA extends BlockPlaceCheck {

    public InteractA(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (shouldSkip()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, place.position, place.getFace())) {
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
        if (shouldSkip()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, place.position, place.getFace())) {
            reward();
            return;
        }

        flagAndAlert("post,pos=" + place.position.getX() + "," + place.position.getY() + "," + place.position.getZ() + ", face=" + place.getFace());
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (shouldSkip()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, blockBreak.position, blockBreak.face)) {
            reward();
            return;
        }

        if (flagAndAlert("break,pos=" + blockBreak.position.getX() + "," + blockBreak.position.getY() + "," + blockBreak.position.getZ() + ", face=" + blockBreak.face)
                && shouldCancel() && shouldModifyPackets()) {
            blockBreak.cancel();
        }
    }

    @Override
    public void onPostFlyingBlockBreak(BlockBreak blockBreak) {
        if (shouldSkip()) {
            return;
        }

        if (InteractVisibilityUtil.isBlockVisible(player, blockBreak.position, blockBreak.face)) {
            reward();
            return;
        }

        flagAndAlert("post_break,pos=" + blockBreak.position.getX() + "," + blockBreak.position.getY() + "," + blockBreak.position.getZ() + ", face=" + blockBreak.face);
    }

    private boolean shouldSkip() {
        return player.gamemode == GameMode.SPECTATOR || player.gamemode == GameMode.ADVENTURE || player.inVehicle();
    }
}
