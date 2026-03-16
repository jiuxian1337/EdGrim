package tech.zkmjnic.edgrim.utils.inventory.inventory;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.inventory.Inventory;
import tech.zkmjnic.edgrim.utils.inventory.InventoryStorage;
import tech.zkmjnic.edgrim.utils.inventory.slot.Slot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

public class BasicInventoryMenu extends AbstractContainerMenu {
    private final int rows;

    public BasicInventoryMenu(EdGrimPlayer player, Inventory playerInventory, int rows) {
        super(player, playerInventory);
        this.rows = rows;

        InventoryStorage containerStorage = new InventoryStorage(rows * 9);

        for (int i = 0; i < rows * 9; i++) {
            addSlot(new Slot(containerStorage, i));
        }

        addFourRowPlayerInventory();
    }

    @Override
    public ItemStack quickMoveStack(int slotID) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotID);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (slotID < this.rows * 9) {
                if (!this.moveItemStackTo(itemstack1, this.rows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.rows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
        }

        return itemstack;
    }
}
