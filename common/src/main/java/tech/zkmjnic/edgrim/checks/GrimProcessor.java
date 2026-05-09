package tech.zkmjnic.edgrim.checks;

import ac.grim.grimac.api.AbstractProcessor;
import ac.grim.grimac.api.config.ConfigReloadable;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.utils.common.ConfigReloadObserver;

public abstract class GrimProcessor implements AbstractProcessor, ConfigReloadable, ConfigReloadObserver {

    // Not everything has to be a check for it to process packets & be configurable

    @Override
    public void reload() {
        reload(EdGrimAPI.INSTANCE.getConfigManager().getConfig());
    }

}
