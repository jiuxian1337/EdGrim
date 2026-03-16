package tech.zkmjnic.edgrim.command.commands;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.command.BuildableCommand;
import tech.zkmjnic.edgrim.command.requirements.PlayerSenderRequirement;
import tech.zkmjnic.edgrim.manager.init.start.CommandRegister;
import tech.zkmjnic.edgrim.platform.api.command.PlayerSelector;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.util.Objects;

public class GrimSpectate implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("edgrim")
                        .literal("spectate")
                        .permission("edgrim.spectate")
                        .required("target", EdGrimAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
                        .handler(this::handleSpectate)
                        .apply(CommandRegister.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    private void handleSpectate(@NonNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        PlayerSelector targetSelectorResults = context.getOrDefault("target", null);
        if (targetSelectorResults == null) return;

        PlatformPlayer targetPlatformPlayer = targetSelectorResults.getSinglePlayer().getPlatformPlayer();

        if (targetPlatformPlayer != null && targetPlatformPlayer.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "cannot-run-on-self", "%prefix% &cYou cannot use this command on yourself!"));
            return;
        }

        if (targetPlatformPlayer != null && targetPlatformPlayer.isExternalPlayer()) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "player-not-this-server", "%prefix% &cThis player isn't on this server!"));
            return;
        }

        @NonNull PlatformPlayer platformPlayer = Objects.requireNonNull(sender.getPlatformPlayer());

        // hide player from tab list
        if (EdGrimAPI.INSTANCE.getSpectateManager().enable(platformPlayer)) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spectate-return", "<click:run_command:/edgrim stopspectating><hover:show_text:\"/edgrim stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>"));
        }

        platformPlayer.setGameMode(GameMode.SPECTATOR);
        platformPlayer.teleportAsync(Objects.requireNonNull(targetPlatformPlayer).getLocation());
    }
}
