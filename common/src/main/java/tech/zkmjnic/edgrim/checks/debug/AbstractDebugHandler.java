package tech.zkmjnic.edgrim.checks.debug;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

public abstract class AbstractDebugHandler extends Check {
    public AbstractDebugHandler(EdGrimPlayer player) {
        super(player);
    }

    public abstract void toggleListener(EdGrimPlayer player);

    public abstract boolean toggleConsoleOutput();
}
