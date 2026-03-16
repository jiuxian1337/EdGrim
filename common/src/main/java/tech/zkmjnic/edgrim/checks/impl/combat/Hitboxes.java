package tech.zkmjnic.edgrim.checks.impl.combat;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

@CheckData(name = "Hitboxes", setback = 10)
public class Hitboxes extends Check {
    public Hitboxes(EdGrimPlayer player) {
        super(player);
    }
}
