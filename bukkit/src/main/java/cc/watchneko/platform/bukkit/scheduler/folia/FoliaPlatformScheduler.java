package cc.watchneko.platform.bukkit.scheduler.folia;

import cc.watchneko.platform.api.scheduler.PlatformScheduler;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

@Getter
public class FoliaPlatformScheduler implements PlatformScheduler {
    private final @NonNull FoliaAsyncScheduler asyncScheduler = new FoliaAsyncScheduler();
    private final @NonNull FoliaGlobalRegionScheduler globalRegionScheduler = new FoliaGlobalRegionScheduler();
    private final @NonNull FoliaEntityScheduler entityScheduler = new FoliaEntityScheduler();
    private final @NonNull FoliaRegionScheduler regionScheduler = new FoliaRegionScheduler();
}
