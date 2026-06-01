package tech.zkmjnic.edgrim.checks.impl.inventory;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.InventoryCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "InventoryG", setback = 3, description = "Sent a entity action packet while inventory is open", experimental = true)
public class InventoryG extends InventoryCheck {

    public InventoryG(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport) return;
        super.onPacketReceive(event);

        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            WrapperPlayClientEntityAction.Action action = wrapper.getAction();

            if (action == WrapperPlayClientEntityAction.Action.STOP_SNEAKING
                    || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                return;
            }

            if (player.hasInventoryOpen) {
                if (flagAndAlert() && !isNoSetbackPermission()) {
                    closeInventory();
                }
            } else {
                reward();
            }
        }
    }
}
