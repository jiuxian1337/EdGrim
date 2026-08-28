package cc.watchneko.platform.api.manager;

import cc.watchneko.platform.api.player.PlatformPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MessagePlaceHolderManager {
    @NonNull
    String replacePlaceholders(@Nullable PlatformPlayer player, @NonNull String string);
}
