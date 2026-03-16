package tech.zkmjnic.edgrim.manager.tick.impl;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.tick.Tickable;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

public class ClearRecentlyUpdatedBlocks implements Tickable {

    private static final int maxTickAge = 2;

    @Override
    public void tick() {
        for (EdGrimPlayer player : EdGrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.blockHistory.cleanup(EdGrimAPI.INSTANCE.getTickManager().currentTick - maxTickAge);
        }
    }
}
