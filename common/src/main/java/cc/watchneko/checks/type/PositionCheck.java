package cc.watchneko.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import cc.watchneko.utils.anticheat.update.PositionUpdate;

public interface PositionCheck extends AbstractCheck {

    default void onPositionUpdate(final PositionUpdate positionUpdate) {
    }
}
