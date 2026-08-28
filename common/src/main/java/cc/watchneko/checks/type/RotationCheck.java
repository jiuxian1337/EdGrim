package cc.watchneko.checks.type;

import cc.watchneko.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends PacketCheck {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
