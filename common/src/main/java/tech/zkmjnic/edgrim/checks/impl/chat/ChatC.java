package tech.zkmjnic.edgrim.checks.impl.chat;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.impl.multiactions.MultiActionsC;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@CheckData(name = "ChatC", description = "Moving while chatting", experimental = true)
public class ChatC extends Check implements PacketCheck {
    public ChatC(PlayerData player) {
        super(player);
    }

    // optionally allow cheats like autogg
    private @Nullable Predicate<String> exemptRegex;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            // TODO make previa after making wrapper parse by client version instead of server version
            check(new WrapperPlayClientChatMessage(event).getMessage(), event);
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {
            check("/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand(), event);
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            // TODO make previa after making wrapper parse by client version instead of server version
            check("/" + new WrapperPlayClientChatCommand(event).getCommand(), event);
        }
    }

    private void check(String message, PacketReceiveEvent event) {
        if (exemptRegex != null && exemptRegex.test(message)) {
            return;
        }

        String verbose = MultiActionsC.getVerbose(player);
        if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        String regexString = config.getStringElse(getConfigName() + ".exempt-regex", null);
        exemptRegex = regexString == null ? null : Pattern.compile(regexString).asMatchPredicate();
    }
}
