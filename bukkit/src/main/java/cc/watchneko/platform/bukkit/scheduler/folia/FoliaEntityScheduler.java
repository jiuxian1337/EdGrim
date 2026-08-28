package cc.watchneko.platform.bukkit.scheduler.folia;

import ac.grim.grimac.api.plugin.GrimPlugin;
import cc.watchneko.platform.api.entity.GrimEntity;
import cc.watchneko.platform.api.scheduler.EntityScheduler;
import cc.watchneko.platform.api.scheduler.TaskHandle;
import cc.watchneko.platform.bukkit.WatchNekoBukkitLoaderPlugin;
import cc.watchneko.platform.bukkit.entity.BukkitEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FoliaEntityScheduler implements EntityScheduler {

    @Override
    public void execute(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delay) {
        ((BukkitEntity) entity).getBukkitEntity().getScheduler().execute(WatchNekoBukkitLoaderPlugin.LOADER, task, retired, delay);
    }

    @Override
    public TaskHandle run(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired) {
        return new FoliaTaskHandle(((BukkitEntity) entity).getBukkitEntity().getScheduler().run(WatchNekoBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired));
    }

    @Override
    public TaskHandle runDelayed(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long delayTicks) {
        return new FoliaTaskHandle(
                ((BukkitEntity) entity).getBukkitEntity().getScheduler().runDelayed(WatchNekoBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, delayTicks)
        );
    }

    @Override
    public TaskHandle runAtFixedRate(@NotNull GrimEntity entity, @NotNull GrimPlugin plugin, @NotNull Runnable task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        return new FoliaTaskHandle(((BukkitEntity) entity).getBukkitEntity().getScheduler().runAtFixedRate(WatchNekoBukkitLoaderPlugin.LOADER, (ignored) -> task.run(), retired, initialDelayTicks, periodTicks));
    }
}
