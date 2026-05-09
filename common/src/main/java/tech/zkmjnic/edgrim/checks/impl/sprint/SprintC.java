package tech.zkmjnic.edgrim.checks.impl.sprint;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

@CheckData(name = "SprintC", description = "Sprinting while using an item", setback = 5, experimental = true)
public class SprintC extends Check implements PostPredictionCheck {
    private boolean flaggedLastTick = false;

    public SprintC(PlayerData player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.packetStateData.isSlowedByUsingItem()) {
            ClientVersion version = player.getClientVersion();

            // https://bugs.mojang.com/browse/MC-152728
            if (version.isNewerThanOrEquals(ClientVersion.V_1_14_2) && version != ClientVersion.V_1_21_4) {
                return;
            }

            if (player.isSprinting && (!player.wasTouchingWater || version.isOlderThan(ClientVersion.V_1_13))) {
                if (flaggedLastTick) flagAndAlertWithSetback();
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
    }
}
