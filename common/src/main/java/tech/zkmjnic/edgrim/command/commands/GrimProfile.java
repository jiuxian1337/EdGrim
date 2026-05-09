package tech.zkmjnic.edgrim.command.commands;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.platform.api.command.PlayerSelector;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;

import java.util.Objects;

public class GrimProfile implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("profile")
                        .permission("edgrim.profile")
                        .required("target", EdGrimAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
                        .handler(this::handleProfile)
        );
    }

    private void handleProfile(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector target = context.get("target");

        PlatformPlayer targetPlatformPlayer = target.getSinglePlayer().getPlatformPlayer();
        if (Objects.requireNonNull(targetPlatformPlayer).isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-this-server", "%prefix% &cThis player isn't on this server!"));
            return;
        }

        PlayerData EdGrimPlayer = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(targetPlatformPlayer.getUniqueId());
        if (EdGrimPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-found", "%prefix% &cPlayer is exempt or offline!"));
            return;
        }

        for (String message : EdGrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("profile")) {
            final Component component = MessageUtil.miniMessage(message);
            sender.sendMessage(MessageUtil.replacePlaceholders(EdGrimPlayer, component));
        }
    }
}
