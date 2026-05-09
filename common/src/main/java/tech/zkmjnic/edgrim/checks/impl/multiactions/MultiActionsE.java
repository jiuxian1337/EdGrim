package tech.zkmjnic.edgrim.checks.impl.multiactions;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "MultiActionsE", description = "Swinging while using an item", experimental = true)
public class MultiActionsE extends Check implements PacketCheck {
    private boolean dropping;

    public MultiActionsE(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!dropping && player.packetStateData.isSlowedByUsingItem() && (player.packetStateData.lastSlotSelected == player.packetStateData.getSlowedByUsingItemSlot() || player.packetStateData.itemInUseHand == InteractionHand.OFF_HAND) && event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            // this is possible to false on 1.7
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                return;
            }

            if (flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }

        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE) {
            dropping = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15)) {
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            dropping = action == DiggingAction.DROP_ITEM || action == DiggingAction.DROP_ITEM_STACK;
        }
    }
}
