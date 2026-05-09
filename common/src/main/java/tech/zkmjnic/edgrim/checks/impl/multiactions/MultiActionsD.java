package tech.zkmjnic.edgrim.checks.impl.multiactions;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "MultiActionsD", description = "Closed inventory while moving", experimental = true)
public class MultiActionsD extends Check implements PacketCheck {
    public MultiActionsD(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            String verbose = MultiActionsC.getVerbose(player);
            if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
