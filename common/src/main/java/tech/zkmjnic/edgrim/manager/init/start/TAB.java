package tech.zkmjnic.edgrim.manager.init.start;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import tech.zkmjnic.edgrim.utils.reflection.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class TAB implements StartableInitable {

    @Override
    public void start() {
        if (EdGrimAPI.INSTANCE.getPluginManager().getPlugin("TAB") == null) return;
        if (!ViaVersionUtil.isAvailable) return;
        // I don't know when team limits were changed, 1.13 is reasonable enough
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13))
            return;

        LogUtil.warn("EdGrim has detected that you have installed TAB with ViaVersion.");
        LogUtil.warn("Please note that currently, TAB is incompatible as it sends illegal packets to players using versions newer than your server version.");
        LogUtil.warn("You may be able to remedy this by setting `compensate-for-packetevents-bug` to true in the TAB config.");
    }
}
