package cc.watchneko.manager.tick.impl;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.manager.tick.Tickable;
import cc.watchneko.player.PlayerData;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        for (PlayerData player : WatchNekoAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            // channel was somehow closed without us getting a disconnect event
            if (!ChannelHelper.isOpen(player.user.getChannel())) {
                WatchNekoAPI.INSTANCE.getPlayerDataManager().onDisconnect(player.user);
                continue;
            }

            player.pollData();
        }
    }
}
