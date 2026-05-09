package tech.zkmjnic.edgrim.platform.bukkit.player;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import tech.zkmjnic.edgrim.platform.api.player.OfflinePlatformPlayer;

import java.util.Objects;
import java.util.UUID;

@lombok.RequiredArgsConstructor
public class BukkitOfflinePlatformPlayer implements OfflinePlatformPlayer {
    private final OfflinePlayer offlinePlayer;

    @Override
    public boolean isOnline() {
        return offlinePlayer.isOnline();
    }

    @Override
    public @NotNull String getName() {
        return Objects.requireNonNull(offlinePlayer.getName());
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OfflinePlatformPlayer player && this.getUniqueId().equals(player.getUniqueId());
    }
}
