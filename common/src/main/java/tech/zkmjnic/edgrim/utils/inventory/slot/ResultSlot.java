package tech.zkmjnic.edgrim.utils.inventory.slot;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.inventory.InventoryStorage;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class ResultSlot extends Slot {

    public ResultSlot(InventoryStorage container, int slot) {
        super(container, slot);
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return false;
    }

    @Override
    public void onTake(EdGrimPlayer player, ItemStack itemStack) {
        // Resync the player's inventory
    }
}
