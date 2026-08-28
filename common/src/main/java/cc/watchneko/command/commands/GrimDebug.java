package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.platform.api.command.PlayerSelector;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

public class GrimDebug implements BuildableCommand {

    public void register(CommandManager<Sender> commandManager) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("watchneko");

        // Register "debug" subcommand
        Command.Builder<Sender> debugCommand = grimCommand
                .literal("debug", Description.of("Toggle debug output for a player"))
                .permission("watchneko.debug")
                .optional("target", WatchNekoAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
                .handler(this::handleDebug);

        // Register "consoledebug" subcommand
        Command.Builder<Sender> consoleDebugCommand = grimCommand
                .literal("consoledebug", Description.of("Toggle console debug output for a player"))
                .permission("watchneko.consoledebug")
                .required("target", WatchNekoAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
                .handler(this::handleConsoleDebug);

        // Register command
        commandManager.command(debugCommand);
        commandManager.command(consoleDebugCommand);
    }

    private void handleDebug(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector playerSelector = context.getOrDefault("target", null);

        PlayerData targetGrimPlayer = parseTarget(sender, playerSelector == null ? sender : playerSelector.getSinglePlayer());
        if (targetGrimPlayer == null) return;

        if (sender.isConsole()) {
            targetGrimPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        } else if (sender.isPlayer()) {
            PlayerData senderGrimPlayer = WatchNekoAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
            targetGrimPlayer.checkManager.getDebugHandler().toggleListener(senderGrimPlayer);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender,
                    "run-as-player-or-console",
                    "%prefix% &cThis command can only be used by players or the console!")
            );
        }
    }

    private void handleConsoleDebug(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetName = context.getOrDefault("target", null);

        PlayerData watchnekoPlayer = parseTarget(sender, targetName.getSinglePlayer());
        if (watchnekoPlayer == null) return;

        boolean isOutput = watchnekoPlayer.checkManager.getDebugHandler().toggleConsoleOutput();
        String playerName = watchnekoPlayer.user.getProfile().getName(); // Use user profile for name

        Component message = Component.text()
                .append(Component.text("Console output for ", NamedTextColor.GRAY))
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" is now ", NamedTextColor.GRAY))
                .append(Component.text(isOutput ? "enabled" : "disabled", NamedTextColor.WHITE))
                .build();

        sender.sendMessage(message);
    }

    private @Nullable PlayerData parseTarget(@NonNull Sender sender, @Nullable Sender t) {
        if (sender.isConsole() && t == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "console-specify-target", "%prefix% &cYou must specify a target as the console!"));
            return null;
        }
        Sender target = t == null ? sender : t;

        PlayerData watchnekoPlayer = WatchNekoAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getUniqueId());
        if (watchnekoPlayer == null) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(sender.getPlatformPlayer().getNative());
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));

            if (user == null) {
                sender.sendMessage(Component.text("Unknown PacketEvents user", NamedTextColor.RED));
            } else {
                boolean isExempt = WatchNekoAPI.INSTANCE.getPlayerDataManager().shouldCheck(user);
                if (!isExempt) {
                    sender.sendMessage(Component.text("User connection state: " + user.getConnectionState(), NamedTextColor.RED));
                }
            }
        }

        return watchnekoPlayer;
    }
}
