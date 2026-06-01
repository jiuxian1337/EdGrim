package tech.zkmjnic.edgrim.checks.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.InventoryCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "InventoryE", setback = 3, description = "Sent a held item change packet while inventory is open")
public class InventoryE extends InventoryCheck {
    private long lastTransaction = Long.MAX_VALUE;

    public InventoryE(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            if (player.hasInventoryOpen) {
                if (this.lastTransaction < player.lastTransactionReceived.get()
                        && flagAndAlert()) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                        player.inventory.needResend = true;
                    }
                    if (!isNoSetbackPermission()) {
                        closeInventory();
                    }
                }
            } else {
                reward();
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.HELD_ITEM_CHANGE) {
            this.lastTransaction = player.lastTransactionSent.get();
        }
    }
}
