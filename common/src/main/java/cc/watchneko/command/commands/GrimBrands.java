package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

import java.util.Objects;

public class GrimBrands implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("brands", Description.of("Toggle brands for the sender"))
                        .permission("watchneko.brand")
                        .handler(this::handleBrands)
        );
    }

    private void handleBrands(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            WatchNekoAPI.INSTANCE.getAlertManager().toggleBrands(Objects.requireNonNull(context.sender().getPlatformPlayer()), false);
        } else if (sender.isConsole()) {
            WatchNekoAPI.INSTANCE.getAlertManager().toggleConsoleBrands();
        }
    }
}
