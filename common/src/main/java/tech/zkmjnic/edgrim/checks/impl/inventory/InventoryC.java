package tech.zkmjnic.edgrim.checks.impl.inventory;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.InventoryCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

@CheckData(name = "InventoryC", setback = 3, description = "Placed a block while inventory is open")
public class InventoryC extends InventoryCheck {

    public InventoryC(PlayerData player) {
        super(player);
    }

    public void onBlockPlace(final BlockPlace place) {
        if (player.hasInventoryOpen) {
            if (flagAndAlert()) {
                if (shouldModifyPackets()) {
                    place.resync();
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
