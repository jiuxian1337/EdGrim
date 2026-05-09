package tech.zkmjnic.edgrim.checks.type;

import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends PacketCheck {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
