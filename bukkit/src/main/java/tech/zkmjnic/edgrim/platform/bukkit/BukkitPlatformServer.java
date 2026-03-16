package tech.zkmjnic.edgrim.platform.bukkit;

import tech.zkmjnic.edgrim.platform.api.PlatformServer;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
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
        CommandSender commandSender = EdGrimBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().reverse(sender);
        Bukkit.dispatchCommand(commandSender, command);
    }

    @Override
    public Sender getConsoleSender() {
        return EdGrimBukkitLoaderPlugin.LOADER.getBukkitSenderFactory().map(Bukkit.getConsoleSender());
    }

    @Override
    public void registerOutgoingPluginChannel(String name) {
        EdGrimBukkitLoaderPlugin.LOADER.getServer().getMessenger().registerOutgoingPluginChannel(EdGrimBukkitLoaderPlugin.LOADER, name);
    }

    @Override
    public double getTPS() {
        return SpigotReflectionUtil.getTPS();
    }
}
