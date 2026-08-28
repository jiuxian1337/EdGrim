package cc.watchneko.utils.inventory.slot;

import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.inventory.InventoryStorage;
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
    public void onTake(PlayerData player, ItemStack itemStack) {
        // Resync the player's inventory
    }
}
