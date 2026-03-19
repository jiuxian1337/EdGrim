package tech.zkmjnic.edgrim.checks.impl.combat;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "Hitboxes", setback = 10)
public class Hitboxes extends Check {
    public Hitboxes(PlayerData player) {
        super(player);
    }
}
