package tech.zkmjnic.edgrim.platform.api.manager;

import tech.zkmjnic.edgrim.platform.api.PlatformPlugin;

public interface PlatformPluginManager {

    PlatformPlugin[] getPlugins();

    PlatformPlugin getPlugin(String pluginName);

    default boolean isPluginEnabled(String pluginName) {
        PlatformPlugin plugin = getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }
}
