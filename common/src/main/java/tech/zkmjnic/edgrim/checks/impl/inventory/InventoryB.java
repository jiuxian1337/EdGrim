package tech.zkmjnic.edgrim.checks.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.InventoryCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "InventoryB", setback = 3, description = "Started digging blocks while inventory is open")
public class InventoryB extends InventoryCheck {
    public InventoryB(PlayerData player) {
        super(player);
    }

    public void handle(PacketReceiveEvent event, WrapperPlayClientPlayerDigging wrapper) {
        if (wrapper.getAction() != DiggingAction.START_DIGGING) return;

        if (player.hasInventoryOpen) {
            if (flagAndAlert()) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
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
