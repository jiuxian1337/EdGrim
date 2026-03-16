package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import tech.zkmjnic.edgrim.platform.api.scheduler.RegionScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.api.world.PlatformWorld;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;
import tech.zkmjnic.edgrim.platform.bukkit.world.BukkitPlatformWorld;
import tech.zkmjnic.edgrim.utils.math.Location;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class FoliaRegionScheduler implements RegionScheduler {

    private final io.papermc.paper.threadedregions.scheduler.RegionScheduler regionScheduler = Bukkit.getRegionScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        regionScheduler.execute(EdGrimBukkitLoaderPlugin.LOADER, ((BukkitPlatformWorld) world).getBukkitWorld(), chunkX, chunkZ, task);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        execute(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return new FoliaTaskHandle(regionScheduler.run(
                EdGrimBukkitLoaderPlugin.LOADER,
                ((BukkitPlatformWorld) world).getBukkitWorld(),
                chunkX,
                chunkZ,
                (ignored) -> task.run()
        ));
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return run(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        return new FoliaTaskHandle(regionScheduler.runDelayed(
                EdGrimBukkitLoaderPlugin.LOADER,
                ((BukkitPlatformWorld) world).getBukkitWorld(),
                chunkX,
                chunkZ,
                (ignored) -> task.run(),
                delayTicks
        ));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return runDelayed(plugin, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task, delayTicks);
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(regionScheduler.runAtFixedRate(
                EdGrimBukkitLoaderPlugin.LOADER,
                ((BukkitPlatformWorld) world).getBukkitWorld(),
                chunkX,
                chunkZ,
                (ignored) -> task.run(),
                initialDelayTicks,
                periodTicks
        ));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return runAtFixedRate(
                plugin,
                location.getWorld(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4,
                task,
                initialDelayTicks,
                periodTicks
        );
    }
}
