package cc.watchneko.utils.nmsutil;

import cc.watchneko.player.PlayerData;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CheckIfChunksLoaded {
    public static boolean isChunksUnloadedAt(PlayerData player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (maxY < player.compensatedWorld.getMinHeight() || minY >= player.compensatedWorld.getMaxHeight()) {
            return true;
        }

        minX >>= 4;
        minZ >>= 4;
        maxX >>= 4;
        maxZ >>= 4;

        for (int i = minX; i <= maxX; ++i) {
            for (int j = minZ; j <= maxZ; ++j) {
                if (player.compensatedWorld.getChunk(i, j) == null) {
                    return true;
                }
            }
        }

        return false;
    }
}
