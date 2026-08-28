package cc.watchneko.checks.type;

import cc.watchneko.player.PlayerData;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import io.netty.channel.Channel;

public class InventoryCheck extends BlockPlaceCheck implements PacketCheck {
    protected static final long NONE = Long.MAX_VALUE;

    protected long closeTransaction = NONE;
    protected int closePacketsToSkip;

    public InventoryCheck(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (closeTransaction != NONE && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
                player.inventory.needResend = true;
            }
        } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            if (closeTransaction != NONE && closePacketsToSkip-- <= 0) {
                closeTransaction = NONE;
            }
        }
    }

    public void closeInventory() {
        if (closeTransaction != NONE) {
            return;
        }

        int windowId = player.inventory.getOpenWindowID();

        player.user.writePacket(new WrapperPlayServerCloseWindow(windowId));
        PacketEvents.getAPI().getProtocolManager().receivePacket(
                player.user.getChannel(), new WrapperPlayClientCloseWindow(windowId)
        );

        player.sendTransaction();
        int transaction = player.lastTransactionSent.get();
        closeTransaction = transaction;
        player.latencyUtils.addRealTimeTask(transaction, () -> {
            if (closeTransaction == transaction) {
                closeTransaction = NONE;
            }
        });

        ((Channel) player.user.getChannel()).flush();
    }
}
