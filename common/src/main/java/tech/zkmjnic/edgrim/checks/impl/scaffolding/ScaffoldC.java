package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

@CheckData(
        name = "ScaffoldC",
        configName = "ScaffoldC",
        decay = 0.86,
        description = "Detects near-vertical pitch (82°/87°) placements while tower scaffolding",
        experimental = true
)
public final class ScaffoldC extends ScaffoldCheck {

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;

    public ScaffoldC(PlayerData player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.pitch = rotationUpdate.getTo().getPitch();
        this.yaw = rotationUpdate.getTo().getYaw();
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock) {
            return;
        }

        cancelPlaceIfWindowActive(place);
//        alert("pitch=" + pitch + "yaw=" + yaw + "lastYaw=" + lastYaw);

//        if (Math.abs(pitch - 82F) < 1.0F || Math.abs(pitch - 87F) < 1.0F) {
//            if (!player.isSneaking) {
//                buffer++;
//            }
//        } else {
//            buffer = Math.max(0, buffer -1F);
//        }
//
//        if (buffer >= 2) {
//            if (flagAndAlert("pitch=" + pitch + "\nyaw=" + yaw + "\nlastYaw=" + lastYaw) && shouldCancel()) {
//                startCancelWindow();
//                place.resync();
//                player.mitigateDamage();
//            }
//        }
        lastYaw = yaw;
        lastPitch = pitch;
    }
}
