package tech.zkmjnic.edgrim.platform.bukkit.initables;

import tech.zkmjnic.edgrim.manager.init.start.StartableInitable;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;
import io.github.retrooper.packetevents.bstats.bukkit.Metrics;

public class BukkitBStats implements StartableInitable {
    @Override
    public void start() {
        int pluginId = 12820; // <-- Replace with the id of your plugin!
        try {
            new Metrics(EdGrimBukkitLoaderPlugin.LOADER, pluginId);
        } catch (Exception ignored) {
        }
    }
}
