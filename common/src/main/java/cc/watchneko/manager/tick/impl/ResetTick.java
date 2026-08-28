package cc.watchneko.manager.tick.impl;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.manager.tick.Tickable;
import cc.watchneko.player.PlayerData;

public class ResetTick implements Tickable {
    @Override
    public void tick() {
        for (PlayerData player : WatchNekoAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.checkManager.getEntityReplication().tickStartTick();
        }
    }
}
