package cc.watchneko.platform.bukkit.initables;

import cc.watchneko.manager.init.start.StartableInitable;
import cc.watchneko.platform.bukkit.WatchNekoBukkitLoaderPlugin;
import cc.watchneko.platform.bukkit.events.DamageMitigationEvent;
import cc.watchneko.platform.bukkit.events.PistonEvent;
import cc.watchneko.utils.anticheat.LogUtil;
import org.bukkit.Bukkit;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Registering bukkit events... (PistonEvent, DamageMitigationEvent)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), WatchNekoBukkitLoaderPlugin.LOADER);
        Bukkit.getPluginManager().registerEvents(new DamageMitigationEvent(), WatchNekoBukkitLoaderPlugin.LOADER);
    }
}
