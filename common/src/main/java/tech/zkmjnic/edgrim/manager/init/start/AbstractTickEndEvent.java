package tech.zkmjnic.edgrim.manager.init.start;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

// Intended for future events we inject all platforms at the end of a tick
public abstract class AbstractTickEndEvent implements StartableInitable {

    @Override
    public void start() {

    }

    protected void onEndOfTick(EdGrimPlayer player) {
        player.checkManager.getEntityReplication().onEndOfTickEvent();
    }

    protected boolean shouldInjectEndTick() {
        return EdGrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("Reach.enable-post-packet", false);


    }
}
