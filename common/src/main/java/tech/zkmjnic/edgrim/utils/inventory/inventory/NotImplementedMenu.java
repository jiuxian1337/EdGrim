package tech.zkmjnic.edgrim.utils.inventory.inventory;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.inventory.Inventory;

public class NotImplementedMenu extends AbstractContainerMenu {
    public NotImplementedMenu(PlayerData player, Inventory playerInventory) {
        super(player, playerInventory);
        player.inventory.isPacketInventoryActive = false;
        player.inventory.needResend = true;
    }

    @Override
    public void doClick(int button, int slotID, WrapperPlayClientClickWindow.WindowClickType clickType) {

    }
}
