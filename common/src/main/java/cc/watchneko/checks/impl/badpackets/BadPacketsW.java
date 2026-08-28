package cc.watchneko.checks.impl.badpackets;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.player.PlayerData;

@CheckData(name = "BadPacketsW", description = "Interacted with non-existent entity", experimental = true)
public class BadPacketsW extends Check {
    public BadPacketsW(PlayerData player) {
        super(player);
    }
}
