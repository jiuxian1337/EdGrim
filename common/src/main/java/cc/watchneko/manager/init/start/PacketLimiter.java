package cc.watchneko.manager.init.start;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.player.PlayerData;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        WatchNekoAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(WatchNekoAPI.INSTANCE.getGrimPlugin(), () -> {
            for (PlayerData player : WatchNekoAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
