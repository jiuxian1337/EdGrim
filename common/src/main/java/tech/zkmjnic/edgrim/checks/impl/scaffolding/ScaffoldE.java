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

        cancelPlaceIfWindowActive(place);
//        alert("pitch=" + pitch + "yaw=" + yaw + "lastYaw=" + lastYaw);

        if (lastSneak != sneak) {
            buffer++;
        } else {
            buffer = Math.max(0, buffer -0.5F);
        }

        if (buffer >= 2) {
            if (flagAndAlert("s=" + sneak + "\nls=" + lastSneak ) && shouldCancel()) {
                startCancelWindow();
                place.resync();
                player.mitigateDamage();
                buffer = 1;
            }
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        lastSneak = sneak;
        sneak = player.isSneaking;
    }
}
