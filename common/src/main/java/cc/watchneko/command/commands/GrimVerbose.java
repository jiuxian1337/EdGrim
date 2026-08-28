package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

public class GrimVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("verbose")
                        .permission("watchneko.verbose")
                        .handler(this::handleVerbose)
        );
    }

    private void handleVerbose(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            WatchNekoAPI.INSTANCE.getAlertManager().toggleVerbose(context.sender().getPlatformPlayer(), false);
        } else if (sender.isConsole()) {
            WatchNekoAPI.INSTANCE.getAlertManager().toggleConsoleVerbose();
        }
    }
}
