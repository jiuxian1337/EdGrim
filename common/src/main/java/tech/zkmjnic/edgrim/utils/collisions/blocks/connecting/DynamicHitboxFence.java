package tech.zkmjnic.edgrim.utils.collisions.blocks.connecting;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.East;
import com.github.retrooper.packetevents.protocol.world.states.enums.North;
import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.github.retrooper.packetevents.protocol.world.states.enums.West;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.collisions.CollisionData;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.CollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.ComplexCollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.HitBoxFactory;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;

public class DynamicHitboxFence extends DynamicConnecting implements HitBoxFactory {
    public static final SimpleCollisionBox[] LEGACY_HITBOXES = new SimpleCollisionBox[]{new SimpleCollisionBox(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D), new SimpleCollisionBox(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 1.0D), new SimpleCollisionBox(0.0D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D), new SimpleCollisionBox(0.0D, 0.0D, 0.375D, 0.625D, 1.0D, 1.0D), new SimpleCollisionBox(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 0.625D), new SimpleCollisionBox(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D), new SimpleCollisionBox(0.0D, 0.0D, 0.0D, 0.625D, 1.0D, 0.625D), new SimpleCollisionBox(0.0D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D), new SimpleCollisionBox(0.375D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D), new SimpleCollisionBox(0.375D, 0.0D, 0.375D, 1.0D, 1.0D, 1.0D), new SimpleCollisionBox(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D), new SimpleCollisionBox(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 1.0D), new SimpleCollisionBox(0.375D, 0.0D, 0.0D, 1.0D, 1.0D, 0.625D), new SimpleCollisionBox(0.375D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D), new SimpleCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.625D), new SimpleCollisionBox(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)};
    private static final CollisionBox[] MODERN_HITBOXES = makeShapes(2.0F, 2.0F, 24.0F, 0.0F, 24.0F, true, 1);
    // no ComplexCollisionBox produced by makeShapes is every larger than 5 SimpleCollisionBoxes
    private static final int MAX_MODERN_HITBOX_COMPLEX_COLLISION_BOX_SIZE = 5;

    static {
        SimpleCollisionBox[] boxes = new SimpleCollisionBox[MAX_MODERN_HITBOX_COMPLEX_COLLISION_BOX_SIZE];

        // we start from one because MODERN_HITBOXES[0] is a NoCollisionBox
        for (int i = 1; i < MODERN_HITBOXES.length; i++) {
            CollisionBox collisionBox = MODERN_HITBOXES[i];
            int size = collisionBox.downCast(boxes);

            for (int j = 0; j < size; j++) {
                if (boxes[j].maxY > 1) {
                    boxes[j].maxY = 1;
                }
            }

            MODERN_HITBOXES[i] = size == 1 ? boxes[0] : new ComplexCollisionBox(size, boxes);
        }
    }

    @Override
    public CollisionBox fetch(PlayerData player, StateType heldItem, ClientVersion version, WrappedBlockState block, boolean isTargetBlock, int x, int y, int z) {
        boolean east;
        boolean north;
        boolean south;
        boolean west;

        // 1.13+ servers on 1.13+ clients send the full fence data
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_13)
                && version.isNewerThanOrEquals(ClientVersion.V_1_13)) {
            east = block.getEast() != East.FALSE;
            north = block.getNorth() != North.FALSE;
            south = block.getSouth() != South.FALSE;
            west = block.getWest() != West.FALSE;
        } else {
            east = connectsTo(player, version, x, y, z, BlockFace.EAST);
            north = connectsTo(player, version, x, y, z, BlockFace.NORTH);
            south = connectsTo(player, version, x, y, z, BlockFace.SOUTH);
            west = connectsTo(player, version, x, y, z, BlockFace.WEST);
        }

        return version.isNewerThanOrEquals(ClientVersion.V_1_12_2)
                ? getModernCollisionBox(north, east, south, west)
                : getLegacyCollisionBox(north, east, south, west);


    }

    private CollisionBox getLegacyCollisionBox(boolean north, boolean east, boolean south, boolean west) {
        return LEGACY_HITBOXES[getAABBIndex(north, east, south, west)].copy();
    }

    private CollisionBox getModernCollisionBox(boolean north, boolean east, boolean south, boolean west) {
        return MODERN_HITBOXES[getAABBIndex(north, east, south, west)].copy();
    }

    @Override
    public boolean checkCanConnect(PlayerData player, WrappedBlockState state, StateType one, StateType two, BlockFace direction) {
        if (BlockTags.FENCES.contains(one))
            return !(one == StateTypes.NETHER_BRICK_FENCE) && !(two == StateTypes.NETHER_BRICK_FENCE);
        else
            return BlockTags.FENCES.contains(one) || CollisionData.getData(one).getMovementCollisionBox(player, player.getClientVersion(), state, 0, 0, 0).isSideFullBlock(direction);
    }
}
