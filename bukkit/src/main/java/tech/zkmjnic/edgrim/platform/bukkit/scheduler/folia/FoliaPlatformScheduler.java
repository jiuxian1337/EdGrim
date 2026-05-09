package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.PlatformScheduler;

@Getter
public class FoliaPlatformScheduler implements PlatformScheduler {
    private final @NonNull FoliaAsyncScheduler asyncScheduler = new FoliaAsyncScheduler();
    private final @NonNull FoliaGlobalRegionScheduler globalRegionScheduler = new FoliaGlobalRegionScheduler();
    private final @NonNull FoliaEntityScheduler entityScheduler = new FoliaEntityScheduler();
    private final @NonNull FoliaRegionScheduler regionScheduler = new FoliaRegionScheduler();
}
