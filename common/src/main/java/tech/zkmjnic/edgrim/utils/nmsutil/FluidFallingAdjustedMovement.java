package tech.zkmjnic.edgrim.utils.nmsutil;

import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class FluidFallingAdjustedMovement {
    public static Vector3dm getFluidFallingAdjustedMovement(@NotNull PlayerData player, double gravity, boolean isFalling, Vector3dm velocity) {
        if (!player.hasGravity || player.isSprinting) return velocity;
        isFalling = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? isFalling : velocity.getY() < 0;
        double newY = isFalling && Math.abs(velocity.getY() - 0.005) >= 0.003 && Math.abs(velocity.getY() - gravity / 16.0) < 0.003 ? -0.003 : velocity.getY() - gravity / 16.0;
        return new Vector3dm(velocity.getX(), newY, velocity.getZ());
    }
}
