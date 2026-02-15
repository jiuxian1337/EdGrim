package ac.grim.grimac.checks.type;

import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

public interface RotationCheck extends PacketCheck  {

    default void process(final RotationUpdate rotationUpdate) {
    }
}
