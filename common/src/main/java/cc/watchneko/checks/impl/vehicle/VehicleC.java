package cc.watchneko.checks.impl.vehicle;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.player.PlayerData;

@CheckData(name = "VehicleC")
public class VehicleC extends Check {
    public VehicleC(PlayerData player) {
        super(player);
    }
}
