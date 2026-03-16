package tech.zkmjnic.edgrim.checks.impl.packetorder;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderG", experimental = true)
public class PacketOrderG extends Check implements PostPredictionCheck {
    public PacketOrderG(EdGrimPlayer player) {
        super(player);
    }

    private final ArrayDeque<String> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING || (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS
                && new WrapperPlayClientClientStatus(event).getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT)) {
            DiggingAction action = null;
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                action = new WrapperPlayClientPlayerDigging(event).getAction();
                if (action == DiggingAction.RELEASE_USE_ITEM
                        || action == DiggingAction.START_DIGGING
                        || action == DiggingAction.CANCELLED_DIGGING
                        || action == DiggingAction.FINISHED_DIGGING
                ) return;
            }

            if (player.packetOrderProcessor.isAttacking()
                    || player.packetOrderProcessor.isReleasing()
                    || player.packetOrderProcessor.isRightClicking()
                    || player.packetOrderProcessor.isPicking()
                    || player.packetOrderProcessor.isDigging()
            ) {
                String verbose = "action=" + (action == null ? "openInventory" : action == DiggingAction.SWAP_ITEM_WITH_OFFHAND ? "swap" : "drop")
                        + ", attacking=" + player.packetOrderProcessor.isAttacking()
                        + ", releasing=" + player.packetOrderProcessor.isReleasing()
                        + ", rightClicking=" + player.packetOrderProcessor.isRightClicking()
                        + ", picking=" + player.packetOrderProcessor.isPicking()
                        + ", digging=" + player.packetOrderProcessor.isDigging();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                flagAndAlert(verbose);
            }
        }

        flags.clear();
    }
}
