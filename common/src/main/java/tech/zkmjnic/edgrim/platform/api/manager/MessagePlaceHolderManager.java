package tech.zkmjnic.edgrim.platform.api.manager;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;

public interface MessagePlaceHolderManager {
    @NonNull
    String replacePlaceholders(@Nullable PlatformPlayer player, @NonNull String string);
}
