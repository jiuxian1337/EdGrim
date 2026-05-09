package tech.zkmjnic.edgrim.checks.impl.packetorder;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

@CheckData(name = "PacketOrderJ", experimental = true)
public class PacketOrderJ extends Check implements PostPredictionCheck {
    private int invalid;

    public PacketOrderJ(final PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (player.packetOrderProcessor.isAttacking() && !player.packetOrderProcessor.isInteracting()) {
                if (!player.canSkipTicks()) {
                    if (flagAndAlert() && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    invalid++;
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
    }
}
