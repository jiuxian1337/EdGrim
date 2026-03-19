package tech.zkmjnic.edgrim.checks.impl.badpackets;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "BadPacketsN", setback = 0)
public class BadPacketsN extends Check {
    public BadPacketsN(final PlayerData player) {
        super(player);
    }
}
