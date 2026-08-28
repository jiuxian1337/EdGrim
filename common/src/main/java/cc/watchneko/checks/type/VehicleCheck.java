package cc.watchneko.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import cc.watchneko.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleCheck extends AbstractCheck {

    void process(final VehiclePositionUpdate vehicleUpdate);
}
