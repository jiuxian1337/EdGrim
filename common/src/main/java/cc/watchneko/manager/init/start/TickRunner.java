package cc.watchneko.manager.init.start;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.platform.api.Platform;
import cc.watchneko.utils.anticheat.LogUtil;

public class TickRunner implements StartableInitable {
    @Override
    public void start() {
        LogUtil.info("Registering tick schedulers...");

        if (WatchNekoAPI.INSTANCE.getPlatform() == Platform.FOLIA) {
            WatchNekoAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(WatchNekoAPI.INSTANCE.getGrimPlugin(), () -> {
                WatchNekoAPI.INSTANCE.getTickManager().tickSync();
                WatchNekoAPI.INSTANCE.getTickManager().tickAsync();
            }, 1, 1);
        } else {
            WatchNekoAPI.INSTANCE.getScheduler().getGlobalRegionScheduler().runAtFixedRate(WatchNekoAPI.INSTANCE.getGrimPlugin(), () -> WatchNekoAPI.INSTANCE.getTickManager().tickSync(), 0, 1);
            WatchNekoAPI.INSTANCE.getScheduler().getAsyncScheduler().runAtFixedRate(WatchNekoAPI.INSTANCE.getGrimPlugin(), () -> WatchNekoAPI.INSTANCE.getTickManager().tickAsync(), 0, 1);
        }
    }
}
