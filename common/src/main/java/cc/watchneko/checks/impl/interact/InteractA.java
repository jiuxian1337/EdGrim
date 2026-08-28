package cc.watchneko.checks.impl.interact;

import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.BlockPlaceCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockBreak;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import cc.watchneko.utils.nmsutil.InteractVisibilityUtil;
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
                && shouldCancel()) {
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
                && shouldCancel()) {
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
