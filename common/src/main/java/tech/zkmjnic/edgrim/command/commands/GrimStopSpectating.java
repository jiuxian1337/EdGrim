package tech.zkmjnic.edgrim.command.commands;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.command.requirements.PlayerSenderRequirement;
import tech.zkmjnic.edgrim.manager.init.start.CommandRegister;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;

import java.util.List;
import java.util.Objects;

public class GrimStopSpectating implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("stopspectating")
                        .permission("edgrim.spectate")
                        .optional("here", StringParser.stringParser(), SuggestionProvider.blocking((ctx, in) -> {
                            if (ctx.sender().hasPermission("edgrim.spectate.stophere")) {
                                return List.of(Suggestion.suggestion("here"));
                            }
                            return List.of(); // No suggestions if no permission
                        }))
                        .handler(this::onStopSpectate)
                        .apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    public void onStopSpectate(CommandContext<Sender> commandContext) {
        Sender sender = commandContext.sender();
        String string = commandContext.getOrDefault("here", null);
        if (EdGrimAPI.INSTANCE.getSpectateManager().isSpectating(sender.getUniqueId())) {
            boolean teleportBack = string == null || !string.equalsIgnoreCase("here") || !sender.hasPermission("edgrim.spectate.stophere");
            EdGrimAPI.INSTANCE.getSpectateManager().disable(Objects.requireNonNull(sender.getPlatformPlayer()), teleportBack);
        } else {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-spectate-return", "%prefix% &cYou can only do this after spectating a player."));
        }
    }
}
