package tech.zkmjnic.edgrim.platform.bukkit.scheduler.folia;

import io.github.retrooper.packetevents.util.folia.TaskWrapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.platform.api.scheduler.TaskHandle;

import java.util.Objects;

public class TaskWrapperHandle implements TaskHandle {

    private final @NotNull TaskWrapper task;

    @Contract(pure = true)
    public TaskWrapperHandle(TaskWrapper task) {
        this.task = Objects.requireNonNull(task);
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelled();
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
