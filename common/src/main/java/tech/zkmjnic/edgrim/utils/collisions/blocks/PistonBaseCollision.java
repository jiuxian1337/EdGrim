package tech.zkmjnic.edgrim.utils.collisions.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.CollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.CollisionFactory;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.HexCollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;

public class PistonBaseCollision implements CollisionFactory {

    @Override
    public CollisionBox fetch(PlayerData player, ClientVersion version, WrappedBlockState block, int x, int y, int z) {
        if (!block.isExtended()) return new SimpleCollisionBox(0, 0, 0, 1, 1, 1, true);

        return switch (block.getFacing()) {
            case UP -> new HexCollisionBox(0, 0, 0, 16, 12, 16);
            case NORTH -> new HexCollisionBox(0, 0, 4, 16, 16, 16);
            case SOUTH -> new HexCollisionBox(0, 0, 0, 16, 16, 12);
            case WEST -> new HexCollisionBox(4, 0, 0, 16, 16, 16);
            case EAST -> new HexCollisionBox(0, 0, 0, 12, 16, 16);
            default -> new HexCollisionBox(0, 4, 0, 16, 16, 16);
        };
    }
}
