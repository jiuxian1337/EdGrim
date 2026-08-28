package cc.watchneko.checks;

import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.api.config.ConfigReloadable;
import cc.watchneko.WatchNekoAPI;
import cc.watchneko.utils.common.ConfigReloadObserver;

public abstract class GrimProcessor implements AbstractProcessor, ConfigReloadable, ConfigReloadObserver {

    // Not everything has to be a check for it to process packets & be configurable

    @Override
    public void reload() {
        reload(WatchNekoAPI.INSTANCE.getConfigManager().getConfig());
    }

}
