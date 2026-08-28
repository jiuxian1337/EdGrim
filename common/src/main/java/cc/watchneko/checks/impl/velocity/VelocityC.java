package cc.watchneko.checks.impl.velocity;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.PostPredictionCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.PredictionComplete;

@CheckData(name = "VelocityC (JumpReset)", configName = "VelocityC")
public class VelocityC extends Check implements PostPredictionCheck {

    public VelocityC(PlayerData player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.actionManager.hasAttackedSince(3500)) {
            buffer = 0;
            return;
        }
        if (player.predictedVelocity.isKnockback() && player.lastOnGround) {
            if (player.predictedVelocity.isJump()) {
                buffer++;
            } else {
                buffer = Math.max(0, buffer - 1);
            }
            if (buffer >= 10) {
                if (flagAndAlert()) {
                    player.mitigateDamage();
                    buffer = 4;
                }
            }
        }
    }
}
