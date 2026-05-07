package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import tech.zkmjnic.edgrim.utils.math.GrimMath;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

@CheckData(
        name = "ScaffoldD",
        configName = "ScaffoldD",
        decay = 0.86,
        description = ""
)
public final class ScaffoldD extends ScaffoldCheck {

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;

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
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }

        cancelPlaceIfWindowActive(place);
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
                buffer = Math.max(0, buffer -1F);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (buffer > 5) {
            if (flagAndAlert("pitch=" + pitch + "\nyaw=" + yaw) && shouldCancel()) {
                startCancelWindow();
                place.resync();
                player.mitigateDamage();
                buffer = 1;
            }
        }
        lastYaw = yaw;
        lastPitch = pitch;
    }

    public static Vector3dm getInferredInput(PlayerData player) {
        VectorData data = player.predictedVelocity;
        if (data != null && data.preUncertainty != null) {
            data = data.preUncertainty;
        }
        while (data != null) {
            if (data.input != null) {
                return data.input;
            }
            data = data.lastVector;
        }
        return null;
    }
}
