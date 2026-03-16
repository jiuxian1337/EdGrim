package tech.zkmjnic.edgrim.checks.impl.vehicle;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

@CheckData(name = "VehicleC")
public class VehicleC extends Check {
    public VehicleC(EdGrimPlayer player) {
        super(player);
    }
}
