package tech.zkmjnic.edgrim.utils.nmsutil;

import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.collisions.CollisionData;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.CollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.ComplexCollisionBox;
import tech.zkmjnic.edgrim.utils.collisions.datatypes.SimpleCollisionBox;
import tech.zkmjnic.edgrim.utils.data.HitData;
import tech.zkmjnic.edgrim.utils.data.Pair;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class InteractVisibilityUtil {
    private static final int DEFAULT_SAMPLES_PER_AXIS = 3;
    private static final double SAMPLE_INSET = 1.0E-4D;

    public static boolean isBlockVisible(EdGrimPlayer player, Vector3i blockPos) {
        return isBoxVisible(player, new SimpleCollisionBox(blockPos));
    }

    public static boolean isBoxVisible(EdGrimPlayer player, SimpleCollisionBox targetBox) {
        final double[] eyeHeights = player.getPossibleEyeHeights();
        final SimpleCollisionBox eyes = getEyeBox(player, eyeHeights);
        if (eyes.isIntersected(targetBox)) {
            return true;
        }

        final List<Vector3dm> samples = sampleBox(targetBox, DEFAULT_SAMPLES_PER_AXIS);
        for (double eyeHeight : eyeHeights) {
            final Vector3dm eye = new Vector3dm(player.x, player.y + eyeHeight, player.z);
            for (Vector3dm sample : samples) {
                if (hasLineOfSight(player, eye, sample, targetBox)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasLineOfSight(EdGrimPlayer player, Vector3dm eye, Vector3dm target, SimpleCollisionBox targetBox) {
        final Vector3i eyeBlock = SimpleCollisionBox.containing(eye.getX(), eye.getY(), eye.getZ());
        final Vector3d start = new Vector3d(eye.getX(), eye.getY(), eye.getZ());
        final Vector3d end = new Vector3d(target.getX(), target.getY(), target.getZ());

        return WorldRayTrace.traverseBlocks(player, start, end, (state, pos) -> {
            if (sameBlock(pos, eyeBlock) || isInsideTarget(pos, targetBox)) {
                return null;
            }
            if (state.getType().isAir() || !segmentHitsBlock(player, state, pos, eye, target)) {
                return null;
            }
            return new HitData(pos, null, null, state);
        }) == null;
    }

    private static boolean segmentHitsBlock(EdGrimPlayer player, WrappedBlockState state, Vector3i pos, Vector3dm start, Vector3dm end) {
        final CollisionBox collisionBox = CollisionData.getData(state.getType()).getMovementCollisionBox(
                player,
                player.getClientVersion(),
                state,
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
        if (collisionBox.isNull()) {
            return false;
        }

        final SimpleCollisionBox[] boxes = new SimpleCollisionBox[ComplexCollisionBox.DEFAULT_MAX_COLLISION_BOX_SIZE];
        final int size = collisionBox.downCast(boxes);
        for (int i = 0; i < size; i++) {
            final SimpleCollisionBox shifted = boxes[i].copy().offset(pos.getX(), pos.getY(), pos.getZ());
            if (ReachUtils.isVecInside(shifted, start)) {
                continue;
            }
            final Pair<Vector3dm, ?> intercept = ReachUtils.calculateIntercept(shifted, start, end);
            if (intercept.first() != null) {
                return true;
            }
        }

        return false;
    }

    private static SimpleCollisionBox getEyeBox(EdGrimPlayer player, double[] eyeHeights) {
        double minEyeHeight = Double.MAX_VALUE;
        double maxEyeHeight = Double.MIN_VALUE;
        for (double height : eyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        final SimpleCollisionBox box = new SimpleCollisionBox(
                player.x, player.y + minEyeHeight, player.z,
                player.x, player.y + maxEyeHeight, player.z
        );
        if (!player.packetStateData.didLastMovementIncludePosition || player.canSkipTicks()) {
            box.expand(player.getMovementThreshold());
        }
        return box;
    }

    private static List<Vector3dm> sampleBox(SimpleCollisionBox box, int samplesPerAxis) {
        final List<Vector3dm> samples = new ArrayList<>(samplesPerAxis * samplesPerAxis * samplesPerAxis);
        final double minX = box.minX + SAMPLE_INSET;
        final double minY = box.minY + SAMPLE_INSET;
        final double minZ = box.minZ + SAMPLE_INSET;
        final double maxX = box.maxX - SAMPLE_INSET;
        final double maxY = box.maxY - SAMPLE_INSET;
        final double maxZ = box.maxZ - SAMPLE_INSET;
        final int steps = Math.max(1, samplesPerAxis - 1);

        for (int x = 0; x < samplesPerAxis; x++) {
            final double sampleX = minX + (maxX - minX) * x / steps;
            for (int y = 0; y < samplesPerAxis; y++) {
                final double sampleY = minY + (maxY - minY) * y / steps;
                for (int z = 0; z < samplesPerAxis; z++) {
                    final double sampleZ = minZ + (maxZ - minZ) * z / steps;
                    samples.add(new Vector3dm(sampleX, sampleY, sampleZ));
                }
            }
        }

        return samples;
    }

    private static boolean isInsideTarget(Vector3i blockPos, SimpleCollisionBox targetBox) {
        for (Vector3i targetPos : SimpleCollisionBox.betweenClosed(targetBox)) {
            if (sameBlock(blockPos, targetPos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameBlock(Vector3i first, Vector3i second) {
        return first.getX() == second.getX() && first.getY() == second.getY() && first.getZ() == second.getZ();
    }
}
