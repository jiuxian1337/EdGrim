package tech.zkmjnic.edgrim.platform.bukkit.initables;

import org.bukkit.Bukkit;
import tech.zkmjnic.edgrim.manager.init.start.StartableInitable;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;
import tech.zkmjnic.edgrim.platform.bukkit.events.DamageMitigationEvent;
import tech.zkmjnic.edgrim.platform.bukkit.events.PistonEvent;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

public class BukkitEventManager implements StartableInitable {
    public void start() {
        LogUtil.info("Registering bukkit events... (PistonEvent, DamageMitigationEvent)");

        Bukkit.getPluginManager().registerEvents(new PistonEvent(), EdGrimBukkitLoaderPlugin.LOADER);
        Bukkit.getPluginManager().registerEvents(new DamageMitigationEvent(), EdGrimBukkitLoaderPlugin.LOADER);
    }
}
