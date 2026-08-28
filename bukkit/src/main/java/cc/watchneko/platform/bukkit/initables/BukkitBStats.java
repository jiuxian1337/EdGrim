package cc.watchneko.platform.bukkit.initables;

import cc.watchneko.manager.init.start.StartableInitable;
import cc.watchneko.platform.bukkit.WatchNekoBukkitLoaderPlugin;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements StartableInitable {
    @Override
    public void start() {
        int pluginId = 12820; // <-- Replace with the id of your plugin!
        try {
            new Metrics(WatchNekoBukkitLoaderPlugin.LOADER, pluginId);
        } catch (Exception ignored) {
        }
    }
}
