package cc.watchneko.utils.blockplace;

import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(PlayerData player, BlockPlace place);
}
