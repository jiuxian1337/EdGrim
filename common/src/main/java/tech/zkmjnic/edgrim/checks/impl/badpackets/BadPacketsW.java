package tech.zkmjnic.edgrim.checks.impl.badpackets;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

@CheckData(name = "BadPacketsW", description = "Interacted with non-existent entity", experimental = true)
public class BadPacketsW extends Check {
    public BadPacketsW(EdGrimPlayer player) {
        super(player);
    }
}
