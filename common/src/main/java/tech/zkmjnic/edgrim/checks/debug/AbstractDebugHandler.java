package tech.zkmjnic.edgrim.checks.debug;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.player.PlayerData;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(PlayerData player) {
        super(player);
    }

    public abstract void toggleListener(PlayerData player);

    public abstract boolean toggleConsoleOutput();
}
