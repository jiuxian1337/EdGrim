package tech.zkmjnic.edgrim.utils.inventory.slot;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.inventory.InventoryStorage;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public void onTake(PlayerData player, ItemStack itemStack) {
        // Resync the player's inventory
    }
}
