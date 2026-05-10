package tech.zkmjnic.edgrim.checks.impl.velocity;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

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
