package tech.zkmjnic.edgrim.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import tech.zkmjnic.edgrim.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleCheck extends AbstractCheck {

    void process(final VehiclePositionUpdate vehicleUpdate);
}
