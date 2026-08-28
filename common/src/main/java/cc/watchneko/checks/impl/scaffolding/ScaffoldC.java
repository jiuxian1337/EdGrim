package cc.watchneko.checks.impl.scaffolding;

import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.ScaffoldCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(
        name = "ScaffoldC",
        configName = "ScaffoldC",
        decay = 0.86,
        description = ""
)
public final class ScaffoldC extends ScaffoldCheck {

    private float dYaw;
    private float dPitch;
    private int lastPlace = 4;

    public ScaffoldC(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.dYaw = rotationUpdate.getDeltaXRotABS();
        this.dPitch = rotationUpdate.getDeltaYRotABS();
        if (lastPlace <= 3) {
            lastPlace++;
            if (dYaw >= 160 || dPitch >= 160) {
                if (flagAndAlert("dy=" + dYaw + "\ndp=" + dPitch + "\nlp=" + lastPlace) && shouldCancel()) {
                    cancel();
                    player.mitigateDamage();
                }
            }
        }
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }
        final BlockFace face = place.getFace();
        if (face == BlockFace.OTHER || face == BlockFace.UP || face == BlockFace.DOWN) return;


        if (dYaw >= 30 || dPitch >= 30) {
            if (!player.isSneaking) {
                buffer++;
            }
        } else {
            buffer = Math.max(0, buffer - 0.5F);
        }

        if (buffer >= 2) {
            if (flagAndAlert("dy=" + dYaw + "\ndp=" + dPitch)) {
                if (shouldCancel()) {
                    cancel();
                    player.mitigateDamage();
                    place.resync();
                }
                buffer = 1;
            }
        }
        lastPlace = 0;
    }
}
