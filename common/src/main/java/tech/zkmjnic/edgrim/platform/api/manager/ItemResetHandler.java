package tech.zkmjnic.edgrim.platform.api.manager;

import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;

public interface ItemResetHandler {
    void resetItemUsage(@Nullable PlatformPlayer player);

    @Contract("null -> null")
    @Nullable InteractionHand getItemUsageHand(@Nullable PlatformPlayer player);
}
