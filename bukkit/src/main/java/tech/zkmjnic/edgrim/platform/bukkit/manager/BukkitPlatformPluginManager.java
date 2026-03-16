package tech.zkmjnic.edgrim.platform.bukkit.manager;

import tech.zkmjnic.edgrim.platform.api.PlatformPlugin;
import tech.zkmjnic.edgrim.platform.api.manager.PlatformPluginManager;
import tech.zkmjnic.edgrim.platform.bukkit.BukkitPlatformPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitPlatformPluginManager implements PlatformPluginManager {

    @Override
    public PlatformPlugin[] getPlugins() {
        Plugin[] bukkitPlugins = Bukkit.getPluginManager().getPlugins();
        PlatformPlugin[] plugins = new PlatformPlugin[bukkitPlugins.length];

        for (int i = 0; i < bukkitPlugins.length; i++) {
            plugins[i] = new BukkitPlatformPlugin(bukkitPlugins[i]);
        }

        return plugins;
    }

    @Override
    public @Nullable PlatformPlugin getPlugin(String pluginName) {
        Plugin bukkitPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return bukkitPlugin == null ? null : new BukkitPlatformPlugin(bukkitPlugin);
    }
}
