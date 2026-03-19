package tech.zkmjnic.edgrim.manager.tick.impl;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.tick.Tickable;
import tech.zkmjnic.edgrim.player.PlayerData;

public class TickInventory implements Tickable {
    @Override
    public void tick() {
        for (PlayerData player : EdGrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            player.inventory.inventory.getInventoryStorage().tickWithBukkit();
        }
    }
}
