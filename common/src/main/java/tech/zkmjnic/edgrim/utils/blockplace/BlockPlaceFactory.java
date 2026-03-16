package tech.zkmjnic.edgrim.utils.blockplace;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

public interface BlockPlaceFactory {
    void applyBlockPlaceToWorld(EdGrimPlayer player, BlockPlace place);
}
