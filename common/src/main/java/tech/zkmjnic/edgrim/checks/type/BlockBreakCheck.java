package tech.zkmjnic.edgrim.checks.type;

import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;

public interface BlockBreakCheck extends PostPredictionCheck {
    default void onBlockBreak(final BlockBreak blockBreak) {
    }

    default void onPostFlyingBlockBreak(final BlockBreak blockBreak) {
    }
}
