package cc.watchneko.checks.debug;

import cc.watchneko.checks.Check;
import cc.watchneko.player.PlayerData;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(PlayerData player) {
        super(player);
    }

    public abstract void toggleListener(PlayerData player);

    public abstract boolean toggleConsoleOutput();
}
