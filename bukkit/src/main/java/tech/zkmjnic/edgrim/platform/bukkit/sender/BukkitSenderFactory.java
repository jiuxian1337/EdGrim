package tech.zkmjnic.edgrim.platform.bukkit.sender;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.platform.api.sender.SenderFactory;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;

import java.util.UUID;

public class BukkitSenderFactory extends SenderFactory<CommandSender> implements SenderMapper<CommandSender, Sender> {
    private final BukkitAudiences audiences;

    public BukkitSenderFactory() {
        this.audiences = BukkitAudiences.create(EdGrimBukkitLoaderPlugin.LOADER);
    }

    @Override
    protected String getName(CommandSender sender) {
        return sender instanceof Player ? sender.getName() : Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        // we can safely send async for players and the console - otherwise, send it sync
        if (sender instanceof Player || sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
            this.audiences.sender(sender).sendMessage(message);
        } else {
            EdGrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().run(
                    EdGrimAPI.INSTANCE.getGrimPlugin(),
                    () -> this.audiences.sender(sender).sendMessage(message)
            );
        }
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node, boolean defaultIfUnset) {
        return sender.hasPermission(new Permission(node, defaultIfUnset ? PermissionDefault.TRUE : PermissionDefault.FALSE));
    }

    @Override
    protected void performCommand(CommandSender sender, String command) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }

    @Override
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    @Override
    public @NonNull Sender map(@NonNull CommandSender base) {
        return this.wrap(base);
    }

    @Override
    public @NonNull CommandSender reverse(@NonNull Sender mapped) {
        return this.unwrap(mapped);
    }
}
