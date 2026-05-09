package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(
        name = "ScaffoldC",
        configName = "ScaffoldC",
        decay = 0.86,
        description = ""
)
public final class ScaffoldC extends ScaffoldCheck {

    private float dYaw;
    private float dPitch;

    public ScaffoldC(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.dYaw = rotationUpdate.getDeltaXRotABS();
        this.dPitch = rotationUpdate.getDeltaYRotABS();
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
            if (flagAndAlert("dy=" + dYaw + "\ndp=" + dPitch) && shouldCancel()) {
                startCancelWindow();
                player.mitigateDamage();
                buffer = 1;
                place.resync();
            }
        }
    }
}
