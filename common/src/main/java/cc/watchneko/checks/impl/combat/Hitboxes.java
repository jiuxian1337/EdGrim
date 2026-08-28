package cc.watchneko.checks.impl.combat;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.player.PlayerData;

@CheckData(name = "Hitboxes", setback = 10)
public class Hitboxes extends Check {
    public Hitboxes(PlayerData player) {
        super(player);
    }
}
