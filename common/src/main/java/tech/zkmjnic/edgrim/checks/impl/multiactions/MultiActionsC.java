package tech.zkmjnic.edgrim.checks.impl.multiactions;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

import java.util.StringJoiner;

@CheckData(name = "MultiActionsC", description = "Clicked in inventory while moving", experimental = true)
public class MultiActionsC extends Check implements PacketCheck {
    public MultiActionsC(PlayerData player) {
        super(player);
    }

    @Contract(pure = true)
    public static String getVerbose(@NotNull PlayerData player) {
        StringJoiner verbose = new StringJoiner(", ");
        if (player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround)) {
            verbose.add("sprinting");
        }

        if (player.isSneaking && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
            verbose.add("sneaking");
        }

        if (player.supportsEndTick() && player.packetStateData.knownInput.moving()) {
            verbose.add("input");
        }

        return verbose.toString();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && !player.serverOpenedInventoryThisTick) {
            String verbose = getVerbose(player);
            if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
