package tech.zkmjnic.edgrim.manager.init.start;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.viaversion.viaversion.api.Via;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import tech.zkmjnic.edgrim.utils.reflection.ViaVersionUtil;

public class ViaVersion implements StartableInitable {

    @Override
    public void start() {
        if (!ViaVersionUtil.isAvailable) return;

        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();

        if (Via.getConfig().getValues().containsKey("fix-1_21-placement-rotation") && Via.getConfig().fix1_21PlacementRotation() && serverVersion.isOlderThan(ServerVersion.V_1_21)) {
            LogUtil.error("EdGrim has detected that you are using ViaVersion with the `fix-1_21-placement-rotation` option enabled.");
            LogUtil.error("This option is known to cause issues with EdGrim and may result in false positives and bypasses.");
            LogUtil.error("Please disable this option in your ViaVersion configuration to prevent these issues.");
        }

        if (EdGrimAPI.INSTANCE.getPluginManager().getPlugin("ViaBackwards") != null && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)) {
            LogUtil.warn("EdGrim has detected that you have installed ViaBackwards on a 1.21.2+ server.");
            LogUtil.warn("This setup is currently unsupported and you will experience issues with older clients using vehicles.");
        }
    }
}
