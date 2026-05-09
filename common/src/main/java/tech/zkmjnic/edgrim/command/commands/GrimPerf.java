package tech.zkmjnic.edgrim.command.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.predictionengine.MovementCheckRunner;

public class GrimPerf {

    public void register(CommandManager<Sender> commandManager) {
        Command.Builder<Sender> grimCommand = commandManager.commandBuilder("edgrim");

        Command.Builder<Sender> configuredBuilder = grimCommand
                .literal("perf", "performance")
                .permission("edgrim.performance")
                .handler(this::handlePerformance);

        commandManager.command(configuredBuilder);
    }

    private void handlePerformance(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        double millis = MovementCheckRunner.predictionNanos / 1000000;
        double longMillis = MovementCheckRunner.longPredictionNanos / 1000000;

        Component message1 = Component.text()
                .append(Component.text("Milliseconds per prediction (avg. 500): ", NamedTextColor.GRAY))
                .append(Component.text(millis, NamedTextColor.WHITE))
                .build();

        Component message2 = Component.text()
                .append(Component.text("Milliseconds per prediction (avg. 20k): ", NamedTextColor.GRAY))
                .append(Component.text(longMillis, NamedTextColor.WHITE))
                .build();

        sender.sendMessage(message1);
        sender.sendMessage(message2);
    }
}
