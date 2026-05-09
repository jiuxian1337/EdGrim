package tech.zkmjnic.edgrim.manager.tick.impl;

import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.tick.Tickable;
import tech.zkmjnic.edgrim.player.PlayerData;

public class ClientVersionSetter implements Tickable {
    @Override
    public void tick() {
        for (PlayerData player : EdGrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            // channel was somehow closed without us getting a disconnect event
            if (!ChannelHelper.isOpen(player.user.getChannel())) {
                EdGrimAPI.INSTANCE.getPlayerDataManager().onDisconnect(player.user);
                continue;
            }

            player.pollData();
        }
    }
}
