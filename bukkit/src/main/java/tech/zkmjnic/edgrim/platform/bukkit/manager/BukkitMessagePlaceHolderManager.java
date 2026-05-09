package tech.zkmjnic.edgrim.platform.bukkit.manager;

import com.github.retrooper.packetevents.util.reflection.Reflection;
import me.clip.placeholderapi.PlaceholderAPI;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import tech.zkmjnic.edgrim.platform.api.manager.MessagePlaceHolderManager;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.platform.bukkit.player.BukkitPlatformPlayer;

public class BukkitMessagePlaceHolderManager implements MessagePlaceHolderManager {
    public static final boolean hasPlaceholderAPI = Reflection.getClassByNameWithoutException("me.clip.placeholderapi.PlaceholderAPI") != null;

    @Override
    public @NonNull String replacePlaceholders(@Nullable PlatformPlayer player, @NonNull String string) {
        if (!hasPlaceholderAPI) return string;
        return PlaceholderAPI.setPlaceholders(player instanceof BukkitPlatformPlayer bukkitPlatformPlayer ? bukkitPlatformPlayer.getBukkitPlayer() : null, string);
    }
}
