package tech.zkmjnic.edgrim.utils.collisions.datatypes;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import tech.zkmjnic.edgrim.player.PlayerData;

public interface HitBoxFactory {
    CollisionBox fetch(PlayerData player, StateType heldItem, ClientVersion version, WrappedBlockState block, boolean isTargetBlock, int x, int y, int z);
}
