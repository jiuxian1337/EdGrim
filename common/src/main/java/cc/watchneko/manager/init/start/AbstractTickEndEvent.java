package cc.watchneko.manager.init.start;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.player.PlayerData;

// Intended for future events we inject all platforms at the end of a tick
public abstract class AbstractTickEndEvent implements StartableInitable {

    @Override
    public void start() {

    }

    protected void onEndOfTick(PlayerData player) {
        player.checkManager.getEntityReplication().onEndOfTickEvent();
    }

    protected boolean shouldInjectEndTick() {
        return WatchNekoAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("Reach.enable-post-packet", false);


    }
}
