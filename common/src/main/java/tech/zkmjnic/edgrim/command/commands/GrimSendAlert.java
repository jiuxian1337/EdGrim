package tech.zkmjnic.edgrim.command.commands;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;

public class GrimSendAlert implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("sendalert")
                        .permission("edgrim.sendalert")
                        .required("message", StringParser.greedyStringParser())
                        .handler(this::handleSendAlert)
        );
    }

    private void handleSendAlert(@NonNull CommandContext<Sender> context) {
        String string = context.get("message");
        string = MessageUtil.replacePlaceholders((Sender) null, string);
        Component message = MessageUtil.miniMessage(string);
        EdGrimAPI.INSTANCE.getAlertManager().sendAlert(message, null);
    }
}
