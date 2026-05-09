package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.AsyncScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;

import java.util.concurrent.TimeUnit;

public class FoliaAsyncScheduler implements AsyncScheduler {

    private final io.papermc.paper.threadedregions.scheduler.AsyncScheduler scheduler = Bukkit.getAsyncScheduler();

    @Override
    public TaskHandle runNow(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new FoliaTaskHandle(scheduler.runNow(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run()));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        return new FoliaTaskHandle(scheduler.runDelayed(
                EdGrimBukkitLoaderPlugin.LOADER,
                (ignored) -> task.run(),
                delay,
                timeUnit
        ));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        return new FoliaTaskHandle(scheduler.runAtFixedRate(
                EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(),
                delay,
                period,
                timeUnit
        ));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(scheduler.runAtFixedRate(
                EdGrimBukkitLoaderPlugin.LOADER,
                (ignored) -> task.run(),
                initialDelayTicks * 50,
                periodTicks * 50,
                TimeUnit.MILLISECONDS
        ));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        scheduler.cancelTasks(EdGrimBukkitLoaderPlugin.LOADER);
    }
}
