package tech.zkmjnic.edgrim.checks.impl.velocity;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

@CheckData(name = "VelocityC (JumpReset)", configName = "VelocityC")
public class VelocityC extends Check implements PostPredictionCheck {

    private int velocitys;
    private int jumpReset;

    public VelocityC(PlayerData player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.actionManager.hasAttackedSince(3500)) {
            velocitys = 0;
            jumpReset = 0;
            return;
        }
        if (player.predictedVelocity.isKnockback() && player.lastOnGround) {
            velocitys++;
            if (player.predictedVelocity.isJump()) {
                jumpReset++;
            }
            double rate = (double) jumpReset / (double) velocitys;
            if (velocitys >= 10 && rate >= 0.5) {
                if (flagAndAlert("rate=" + rate)) {
                    player.mitigateDamage();
                }
            }
        }
    }
}
