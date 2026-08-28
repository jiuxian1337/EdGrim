package cc.watchneko.command.commands;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.command.BuildableCommand;
import cc.watchneko.command.requirements.PlayerSenderRequirement;
import cc.watchneko.manager.init.start.CommandRegister;
import cc.watchneko.platform.api.command.PlayerSelector;
import cc.watchneko.platform.api.player.PlatformPlayer;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;

import java.util.Objects;

public class GrimSpectate implements BuildableCommand {
    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("watchneko")
                        .literal("spectate")
                        .permission("watchneko.spectate")
                        .required("target", WatchNekoAPI.INSTANCE.getCommandAdapter().singlePlayerSelectorParser())
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
        if (WatchNekoAPI.INSTANCE.getSpectateManager().enable(platformPlayer)) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "spectate-return", "<click:run_command:/watchneko stopspectating><hover:show_text:\"/watchneko stopspectating\">\n%prefix% &fClick here to return to previous location\n</hover></click>"));
        }

        platformPlayer.setGameMode(GameMode.SPECTATOR);
        platformPlayer.teleportAsync(Objects.requireNonNull(targetPlatformPlayer).getLocation());
    }
}
