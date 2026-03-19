package tech.zkmjnic.edgrim.utils.collisions.datatypes;

import tech.zkmjnic.edgrim.player.PlayerData;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public interface CollisionFactory {
    CollisionBox fetch(PlayerData player, ClientVersion version, WrappedBlockState block, int x, int y, int z);
}
