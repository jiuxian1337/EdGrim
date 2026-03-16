package tech.zkmjnic.edgrim.manager.init.start;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.Platform;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

public class TickRunner implements StartableInitable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (EdGrimAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            EdGrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(EdGrimAPI.INSTANCE.getGrimPlugin(), () -> {
                EdGrimAPI.INSTANCE.getTickManager().tickSync();
                EdGrimAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            EdGrimAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(EdGrimAPI.INSTANCE.getGrimPlugin(), () -> EdGrimAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            EdGrimAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(EdGrimAPI.INSTANCE.getGrimPlugin(), () -> EdGrimAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
