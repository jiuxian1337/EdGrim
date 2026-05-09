package tech.zkmjnic.edgrim.command.commands;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;

public class GrimHelp implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("help", Description.of("Display help information"))
                        .permission("edgrim.help")
                        .handler(this::handleHelp)
        );
    }

    private void handleHelp(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();

        for (String string : EdGrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("help")) {
            if (string == null) continue;
            string = MessageUtil.replacePlaceholders(sender, string);
            sender.sendMessage(MessageUtil.miniMessage(string));
        }
    }
}
