package tech.zkmjnic.edgrim.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import tech.zkmjnic.edgrim.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
