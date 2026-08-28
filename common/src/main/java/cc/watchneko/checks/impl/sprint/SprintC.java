package cc.watchneko.checks.impl.sprint;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.PostPredictionCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

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
