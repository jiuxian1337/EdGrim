package tech.zkmjnic.edgrim.command.commands;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

import java.util.Objects;

public class GrimBrands implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("brands", Description.of("Toggle brands for the sender"))
                        .permission("edgrim.brand")
                        .handler(this::handleBrands)
        );
    }

    private void handleBrands(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        if (sender.isPlayer()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleBrands(Objects.requireNonNull(context.sender().getPlatformPlayer()), false);
        } else if (sender.isConsole()) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleConsoleBrands();
        }
    }
}
