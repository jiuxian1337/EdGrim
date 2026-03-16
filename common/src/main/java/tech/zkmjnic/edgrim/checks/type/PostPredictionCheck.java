package tech.zkmjnic.edgrim.checks.type;

import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

public interface PostPredictionCheck extends PacketCheck {

    default void onPredictionComplete(final PredictionComplete predictionComplete) {
    }
}
