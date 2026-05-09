package tech.zkmjnic.edgrim.platform.bukkit.scheduler.bukkit;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.PlatformScheduler;

@Getter
public class BukkitPlatformScheduler implements PlatformScheduler {
    private final @NonNull BukkitAsyncScheduler asyncScheduler = new BukkitAsyncScheduler();
    private final @NonNull BukkitGlobalRegionScheduler globalRegionScheduler = new BukkitGlobalRegionScheduler();
    private final @NonNull BukkitEntityScheduler entityScheduler = new BukkitEntityScheduler();
    private final @NonNull BukkitRegionScheduler regionScheduler = new BukkitRegionScheduler();
}
