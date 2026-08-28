package cc.watchneko.platform.bukkit;

import cc.watchneko.platform.api.PlatformServer;
import cc.watchneko.platform.api.sender.Sender;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;


public class BukkitPlatformServer implements PlatformServer {

    @Override
    public String getPlatformImplementationString() {
        return Bukkit.getVersion();
    }

    @Override
    public void dispatchCommand(Sender sender, String command) {
        CommandSender commandSender = WatchNekoBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().reverse(sender);
        Bukkit.dispatchCommand(commandSender, command);
    }

    @Override
    public Sender getConsoleSender() {
        return WatchNekoBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(Bukkit.getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        WatchNekoBukkitLoaderPlugin.LOADER.getServer().getMessenger().registerOutgoingPluginChannel(WatchNekoBukkitLoaderPlugin.LOADER, name);
    }

    @Override
    public double getTPS() {
        return SpigotReflectionUtil.getTPS();
    }
}
