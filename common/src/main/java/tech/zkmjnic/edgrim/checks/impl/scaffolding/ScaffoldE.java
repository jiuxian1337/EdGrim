package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

@CheckData(
        name = "ScaffoldE",
        configName = "ScaffoldE",
        decay = 0.86,
        description = ""
)
public final class ScaffoldE extends ScaffoldCheck {

    private boolean sneak;
    private boolean lastSneak;
    private int placeCount;

    public ScaffoldE(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }
        final BlockFace face = place.getFace();
        if (face == BlockFace.OTHER || face == BlockFace.UP || face == BlockFace.DOWN) return;

        placeCount++;
        if (lastSneak != sneak) {
            buffer++;
        }

        double v = buffer / placeCount;
        if (v > 0.4 && placeCount > 4) {
            if (flagAndAlert("s=" + lastSneak + "\ns=" + sneak + "\nf/pc=" + v)) {
                placeCount = 0;
                buffer = 0;
                if (shouldCancel()) {
                    cancel();
                    player.mitigateDamage();
                    place.resync();
                }
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        lastSneak = sneak;
        sneak = player.isSneaking;
    }
}
