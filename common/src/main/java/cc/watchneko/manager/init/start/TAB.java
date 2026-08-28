package cc.watchneko.manager.init.start;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.utils.anticheat.LogUtil;
import cc.watchneko.utils.reflection.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class TAB implements StartableInitable {

    @Override
    public void start() {
        if (WatchNekoAPI.INSTANCE.getPluginManager().getPlugin("TAB") == null) return;
        if (!ViaVersionUtil.isAvailable) return;
        // I don't know when team limits were changed, 1.13 is reasonable enough
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13))
            return;

        LogUtil.warn("WatchNeko has detected that you have installed TAB with ViaVersion.");
        LogUtil.warn("Please note that currently, TAB is incompatible as it sends illegal packets to players using versions newer than your server version.");
        LogUtil.warn("You may be able to remedy this by setting `compensate-for-packetevents-bug` to true in the TAB config.");
    }
}
