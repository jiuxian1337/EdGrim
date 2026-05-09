package tech.zkmjnic.edgrim.utils.data;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

public record HitData(
        Vector3i position,
        Vector3dm blockHitLocation,
        BlockFace closestDirection,
        WrappedBlockState state
) {
    public Vector3d getRelativeBlockHitLocation() {
        return new Vector3d(blockHitLocation.getX() - position.getX(), blockHitLocation.getY() - position.getY(), blockHitLocation.getZ() - position.getZ());
    }
}
