package tech.zkmjnic.edgrim.command.commands;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;

import java.util.Objects;

public class GrimAlerts implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("alerts", Description.of("Toggle alerts for the sender"))
                        .permission("edgrim.alerts")
                        .handler(this::handleAlerts)
        );
    }

    // Suppress warning as we've already checked sender is not console
    private void handleAlerts(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleAlerts(Objects.requireNonNull(context.sender().getPlatformPlayer()), false);
        } else if (sender.isConsole()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleConsoleAlerts();
        }
    }
}
