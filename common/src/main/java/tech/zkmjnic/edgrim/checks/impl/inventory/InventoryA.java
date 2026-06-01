package tech.zkmjnic.edgrim.checks.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.InventoryCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "InventoryA", setback = 3, description = "Attacked an entity while inventory is open")
public class InventoryA extends InventoryCheck {
    public InventoryA(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() != InteractAction.ATTACK) return;

            if (player.hasInventoryOpen) {
                if (flagAndAlert()) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    if (!isNoSetbackPermission())
                        closeInventory();
                }
            } else {
                reward();
            }
        }
    }
}
