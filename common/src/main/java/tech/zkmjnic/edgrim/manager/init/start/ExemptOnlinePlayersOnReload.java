package tech.zkmjnic.edgrim.manager.init.start;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;

public class ExemptOnlinePlayersOnReload implements StartableInitable {

    // Runs on plugin startup adding all online players to exempt list; will be empty unless reload
    // This essentially exists to stop you from shooting yourself in the foot by being stupid and using /reload
    @Override
    public void start() {
        for (PlatformPlayer player : EdGrimAPI.INSTANCE.getPlatformPlayerFactory().getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player.getNative());
            EdGrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.add(user);
        }
    }
}
