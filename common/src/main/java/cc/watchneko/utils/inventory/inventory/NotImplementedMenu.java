package cc.watchneko.utils.inventory.inventory;

import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.inventory.Inventory;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

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
