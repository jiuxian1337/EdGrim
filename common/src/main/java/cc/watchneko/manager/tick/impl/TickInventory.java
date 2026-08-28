package cc.watchneko.manager.tick.impl;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.manager.tick.Tickable;
import cc.watchneko.player.PlayerData;

public class TickInventory implements Tickable {
    @Override
    public void tick() {
        for (PlayerData player : WatchNekoAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.inventory.inventory.getInventoryStorage().tickWithBukkit();
        }
    }
}
