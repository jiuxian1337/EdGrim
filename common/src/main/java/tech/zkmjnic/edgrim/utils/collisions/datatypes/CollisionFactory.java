package tech.zkmjnic.edgrim.utils.collisions.datatypes;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import tech.zkmjnic.edgrim.player.PlayerData;

public interface CollisionFactory {
    CollisionBox fetch(PlayerData player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
