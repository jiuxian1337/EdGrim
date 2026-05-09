package tech.zkmjnic.edgrim.checks.impl.crash;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "CrashI")
public class CrashI extends Check implements PacketCheck {
    public CrashI(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SELECT_BUNDLE_ITEM) {
            int selectedItemIndex;
            try {
                selectedItemIndex = new WrapperPlayClientSelectBundleItem(event).getSelectedItemIndex();
            } catch (IllegalArgumentException e) {
                // thanks packetevents!
                if (e.getMessage().startsWith("Invalid selectedItemIndex: ")) {
                    selectedItemIndex = Integer.parseInt(e.getMessage().substring(27));
                } else {
                    throw e;
                }
            }

            if (selectedItemIndex < -1) {
                flagAndAlert("selectedItemIndex=" + selectedItemIndex);
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
