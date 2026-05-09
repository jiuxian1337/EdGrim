package tech.zkmjnic.edgrim.platform.bukkit.scheduler.bukkit;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.zkmjnic.edgrim.platform.api.entity.GrimEntity;
import tech.zkmjnic.edgrim.platform.api.scheduler.EntityScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;

public class BukkitEntityScheduler implements EntityScheduler {
    private final BukkitScheduler scheduler = Bukkit.getScheduler();

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        scheduler.runTaskLater(EdGrimBukkitLoaderPlugin.LOADER, run, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new BukkitTaskHandle(scheduler.runTask(EdGrimBukkitLoaderPlugin.LOADER, task));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new BukkitTaskHandle(scheduler.runTaskLater(EdGrimBukkitLoaderPlugin.LOADER, task, delayTicks));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new BukkitTaskHandle(scheduler.runTaskTimer(EdGrimBukkitLoaderPlugin.LOADER, task, initialDelayTicks, periodTicks));
    }
}
