package tech.zkmjnic.edgrim.checks.impl.sprint;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

@CheckData(name = "SprintF", description = "Sprinting while gliding", experimental = true)
public class SprintF extends Check implements PostPredictionCheck {
    public SprintF(PlayerData player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.wasGliding && player.isGliding && player.getClientVersion() == ClientVersion.V_1_21_4) {
            if (player.isSprinting) {
                flagAndAlertWithSetback();
            } else {
                reward();
            }
        }
    }
}
