package cc.watchneko.checks.impl.breaking;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.BlockBreakCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockBreak;
import cc.watchneko.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "PositionBreakA")
public class PositionBreakA extends Check implements BlockBreakCheck {
    public PositionBreakA(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (player.inVehicle()
                || blockBreak.action == DiggingAction.CANCELLED_DIGGING
                || blockBreak.block.getType() == StateTypes.REDSTONE_WIRE
        ) return;

        SimpleCollisionBox combined = blockBreak.getCombinedBox();

        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        SimpleCollisionBox eyePositions = new SimpleCollisionBox(player.x, player.y + minEyeHeight, player.z, player.x, player.y + maxEyeHeight, player.z);
        if (!player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks()) {
            eyePositions.expand(player.getMovementThreshold());
        }

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(combined)) {
            return;
        }

        // So now we have the player's possible eye positions
        // So then look at the face that the player has clicked
        boolean flag = switch (blockBreak.face) {
            case NORTH -> eyePositions.minZ > combined.minZ; // Z- face
            case SOUTH -> eyePositions.maxZ < combined.maxZ; // Z+ face
            case EAST -> eyePositions.maxX < combined.maxX; // X+ face
            case WEST -> eyePositions.minX > combined.minX; // X- face
            case UP -> eyePositions.maxY < combined.maxY; // Y+ face
            case DOWN -> eyePositions.minY > combined.minY; // Y- face
            default -> false;
        };

        if (flag && flagAndAlert("action=" + blockBreak.action + ", face=" + blockBreak.face) && shouldModifyPackets()) {
            blockBreak.cancel();
        }
    }
}
