package cc.watchneko.manager.tick.impl;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.manager.tick.Tickable;
import cc.watchneko.player.PlayerData;

public class ClearRecentlyUpdatedBlocks implements Tickable {

    private static final int maxTickAge = 2;

    @Override
    public void tick() {
        for (PlayerData player : WatchNekoAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.blockHistory.cleanup(WatchNekoAPI.INSTANCE.getTickManager().currentTick - maxTickAge);
        }
    }
}
