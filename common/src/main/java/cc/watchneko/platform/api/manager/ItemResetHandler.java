package cc.watchneko.platform.api.manager;

import cc.watchneko.platform.api.player.PlatformPlayer;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface ItemResetHandler {
    void resetItemUsage(@Nullable PlatformPlayer player);

    @Contract("null -> null")
    @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer player);
}
