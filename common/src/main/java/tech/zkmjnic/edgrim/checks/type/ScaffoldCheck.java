package tech.zkmjnic.edgrim.checks.type;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.player.PlayerData;

public class ScaffoldCheck extends BlockPlaceCheck {
    protected long cancelForMs;
    protected static long cancelPlacementsUntil;

    public ScaffoldCheck(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        this.cancelForMs = config.getLongElse(getConfigName() + ".cancel-for-ms", 500L);
    }

    protected void cancel() {
        if (cancelForMs <= 0L) {
            return;
        }
        cancelPlacementsUntil = Math.max(cancelPlacementsUntil + cancelForMs, System.currentTimeMillis() + cancelForMs);
    }
}
