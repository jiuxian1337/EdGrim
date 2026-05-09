package tech.zkmjnic.edgrim.command.commands;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;

public class GrimVerbose implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("verbose")
                        .permission("edgrim.verbose")
                        .handler(this::handleVerbose)
        );
    }

    private void handleVerbose(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleVerbose(context.sender().getPlatformPlayer(), false);
        } else if (sender.isConsole()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleConsoleVerbose();
        }
    }
}
