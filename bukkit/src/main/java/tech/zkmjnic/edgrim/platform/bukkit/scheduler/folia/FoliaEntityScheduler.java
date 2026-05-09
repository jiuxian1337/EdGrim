package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.zkmjnic.edgrim.platform.api.entity.GrimEntity;
import tech.zkmjnic.edgrim.platform.api.scheduler.EntityScheduler;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;
import tech.zkmjnic.edgrim.platform.bukkit.EdGrimBukkitLoaderPlugin;
import tech.zkmjnic.edgrim.platform.bukkit.entity.BukkitEdGrimEntity;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delay) {
        ((BukkitEdGrimEntity) entity).getBukkitEntity().getScheduler().execute(EdGrimBukkitLoaderPlugin.LOADER, task, retired, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new FoliaTaskHandle(((BukkitEdGrimEntity) entity).getBukkitEntity().getScheduler().run(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new FoliaTaskHandle(
                ((BukkitEdGrimEntity) entity).getBukkitEntity().getScheduler().runDelayed(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, delayTicks)
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(((BukkitEdGrimEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(EdGrimBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, initialDelayTicks, periodTicks));
    }
}
