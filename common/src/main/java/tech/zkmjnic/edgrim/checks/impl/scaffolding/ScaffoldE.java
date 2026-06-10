package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;

import java.util.ArrayList;
import java.util.List;

@CheckData(
        name = "ScaffoldE",
        configName = "ScaffoldE",
        decay = 0.86,
        description = ""
)
public final class ScaffoldE extends ScaffoldCheck {

    private boolean sneak;
    private boolean lastSneak;
    private List<Boolean> place = new ArrayList<>();

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

        if (lastSneak != sneak) {
            this.place.add(true);
        } else {
            this.place.add(false);
        }

        if (this.place.size() >= 10) {
            int trueCount = 0;
            for (Boolean changed : this.place) {
                if (changed) trueCount++;
            }

            double v = (double) trueCount / this.place.size();

            if (v > 0.6 && flagAndAlert("s=" + lastSneak + "\ns=" + sneak + "\nt/c=" + v) && shouldCancel()) {
                cancel();
                player.mitigateDamage();
                place.resync();
            }
            this.place.remove(0);
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        lastSneak = sneak;
        sneak = player.isSneaking;
    }
}
