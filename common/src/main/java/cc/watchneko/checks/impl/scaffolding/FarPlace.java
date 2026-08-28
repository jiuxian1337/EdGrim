package cc.watchneko.checks.impl.scaffolding;

import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.BlockPlaceCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import cc.watchneko.utils.collisions.datatypes.SimpleCollisionBox;
import cc.watchneko.utils.math.Vector3dm;
import cc.watchneko.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;

@CheckData(name = "FarPlace", description = "Placing blocks from too far away")
public class FarPlace extends BlockPlaceCheck {
    public FarPlace(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.SPECTATOR || player.inVehicle()) return;

        Vector3i blockPos = place.position;

        if (place.material == StateTypes.SCAFFOLDING) return;

        double min = Double.MAX_VALUE;
        final double[] possibleEyeHeights = player.getPossibleEyeHeights();
        for (double d : possibleEyeHeights) {
            SimpleCollisionBox box = new SimpleCollisionBox(blockPos);
            Vector3dm eyes = new Vector3dm(player.x, player.y + d, player.z);
            Vector3dm best = VectorUtils.cutBoxToVector(eyes, box);
            min = Math.min(min, eyes.distanceSquared(best));
        }

        // getPickRange() determines this?
        // With 1.20.5+ the new attribute determines creative mode reach using a modifier
        double maxReach = player.compensatedEntities.self.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double threshold = player.getMovementThreshold();
        maxReach += Math.hypot(threshold, threshold);

        if (min > maxReach * maxReach) { // fail
            if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
