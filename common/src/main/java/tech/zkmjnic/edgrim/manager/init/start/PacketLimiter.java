package tech.zkmjnic.edgrim.manager.init.start;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.player.PlayerData;

public class PacketLimiter implements StartableInitable {
    @Override
    public void start() {
        EdGrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(EdGrimAPI.INSTANCE.getGrimPlugin(), () -> {
            for (PlayerData player : EdGrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
                // Avoid concurrent reading on an integer as it's results are unknown
                player.cancelledPackets.set(0);
            }
        }, 1, 20);
    }
}
