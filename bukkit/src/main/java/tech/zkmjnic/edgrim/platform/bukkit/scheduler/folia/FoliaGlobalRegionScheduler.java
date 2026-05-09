package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.GlobalRegionScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;

public class FoliaGlobalRegionScheduler implements GlobalRegionScheduler {

    private final io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler globalRegionScheduler = Bukkit.getGlobalRegionScheduler();

    @Override
    public void execute(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        globalRegionScheduler.execute(EdGrimBukkitLoaderPlugin.LOADER, task);
    }

    @Override
    public TaskHandle run(@NotNull GrimPlugin plugin, @NotNull Runnable task) {
        return new FoliaTaskHandle(globalRegionScheduler.run(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run()));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimPlugin plugin, @NotNull Runnable task, long delay) {
        return new FoliaTaskHandle(globalRegionScheduler.runDelayed(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), delay));
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimPlugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(globalRegionScheduler.runAtFixedRate(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), initialDelayTicks, periodTicks));
    }

    @Override
    public void cancel(@NotNull GrimPlugin plugin) {
        globalRegionScheduler.cancelTasks(EdGrimBukkitLoaderPlugin.LOADER);
    }
}
