package tech.zkmjnic.edgrim.checks.impl.multiactions;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockBreakCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;

@CheckData(name = "MultiActionsB", description = "Breaking blocks while using an item", experimental = true)
public class MultiActionsB extends Check implements BlockBreakCheck {
    public MultiActionsB(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.packetStateData.isSlowedByUsingItem() && (player.packetStateData.lastSlotSelected == player.packetStateData.getSlowedByUsingItemSlot() || player.packetStateData.itemInUseHand == InteractionHand.OFF_HAND)) {
            // this is vanilla on 1.7
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                return;
            }

            if (flagAndAlert() && shouldModifyPackets()) {
                blockBreak.cancel();
            }
        }
    }
}
