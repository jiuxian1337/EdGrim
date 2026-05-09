package tech.zkmjnic.edgrim.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.RegionScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.api.world.PlatformWorld;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;
import tech.zkmjnic.edgrim.utils.math.Location;

public class BukkitRegionScheduler implements RegionScheduler {

    private final BukkitScheduler bukkitScheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        bukkitScheduler.runTask(EdGrimBukkitLoaderPlugin.LOADER, task);
    }

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        bukkitScheduler.runTask(EdGrimBukkitLoaderPlugin.LOADER, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(EdGrimBukkitLoaderPlugin.LOADER, task));
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return new BukkitTaskHandle(bukkitScheduler.runTask(EdGrimBukkitLoaderPlugin.LOADER, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long delayTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(EdGrimBukkitLoaderPlugin.LOADER, task, delayTicks));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskLater(EdGrimBukkitLoaderPlugin.LOADER, task, delayTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull PlatformWorld world, int chunkX, int chunkZ, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(EdGrimBukkitLoaderPlugin.LOADER, task, initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Location location, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(bukkitScheduler.runTaskTimer(EdGrimBukkitLoaderPlugin.LOADER, task, initialDelayTicks, periodTicks));
    }
}
