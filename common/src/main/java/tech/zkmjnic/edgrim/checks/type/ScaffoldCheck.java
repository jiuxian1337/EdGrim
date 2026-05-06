package tech.zkmjnic.edgrim.checks.type;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

public class ScaffoldCheck extends BlockPlaceCheck {
    protected long cancelForMs;
    private long cancelPlacementsUntil;

    public ScaffoldCheck(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        this.cancelForMs = config.getLongElse(getConfigName() + ".cancel-for-ms", 500L);
    }

    protected void startCancelWindow() {
        if (cancelForMs <= 0L) {
            return;
        }
        cancelPlacementsUntil = Math.max(cancelPlacementsUntil + cancelForMs, System.currentTimeMillis() + cancelForMs);
    }

    protected boolean isCancelWindowActive() {
        if (cancelPlacementsUntil == 0L) {
            return false;
        }
        if (System.currentTimeMillis() >= cancelPlacementsUntil) {
            cancelPlacementsUntil = 0L;
            return false;
        }
        return true;
    }

    protected boolean cancelPlaceIfWindowActive(BlockPlace place) {
        if (!place.isBlock || !shouldModifyPackets() || !isCancelWindowActive()) {
            return false;
        }
        place.resync();
        return true;
    }
}
