package tech.zkmjnic.edgrim.command.requirements;

import tech.zkmjnic.edgrim.command.SenderRequirement;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;

public final class PlayerSenderRequirement implements SenderRequirement {

    public static final PlayerSenderRequirement PLAYER_SENDER_REQUIREMENT = new PlayerSenderRequirement();

    @Override
    public @NonNull Component errorMessage(Sender sender) {
        return MessageUtil.getParsedComponent(sender, "run-as-player", "%prefix% &cThis command can only be used by players!");
    }

    @Override
    public boolean evaluateRequirement(@NonNull CommandContext<Sender> commandContext) {
        return commandContext.sender().isPlayer();
    }
}
