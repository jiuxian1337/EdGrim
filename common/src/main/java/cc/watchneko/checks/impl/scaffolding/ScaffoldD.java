package cc.watchneko.checks.impl.scaffolding;

import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.ScaffoldCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import cc.watchneko.utils.anticheat.update.RotationUpdate;
import cc.watchneko.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(
        name = "ScaffoldD",
        configName = "ScaffoldD",
        decay = 0.86,
        description = ""
)
public final class ScaffoldD extends ScaffoldCheck {

    private float yaw;
    private float pitch;

    public ScaffoldD(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.pitch = rotationUpdate.getTo().getPitch();
        this.yaw = rotationUpdate.getTo().getYaw();
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        try {
            if (!place.isBlock || place.position.y >= player.y) {
                return;
            }
//        alert("pitch=" + pitch + "yaw=" + yaw + "lastYaw=" + lastYaw);

            try {
                final BlockFace face = place.getFace();
                if (getInferredInput(player).getX() == 0 && getInferredInput(player).getZ() < 0 &&
                        Math.abs((GrimMath.wrapAngleTo180(yaw) % 90) - 45) > 22.5F &&
                        face != BlockFace.OTHER && face != BlockFace.UP && face != BlockFace.DOWN) {
                    if (!player.isSneaking && !player.isJumping && player.onGround) {
                        buffer++;
                    } else {
                        buffer = Math.max(0, buffer - 0.25F);
                    }
                } else {
                    buffer = Math.max(0, buffer - 1F);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (buffer > 5) {
                if (flagAndAlert("pitch=" + pitch + "\nyaw=" + yaw)) {
                    if (shouldCancel()) {
                        cancel();
                        player.mitigateDamage();
                        place.resync();
                    }
                    buffer = 1;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
