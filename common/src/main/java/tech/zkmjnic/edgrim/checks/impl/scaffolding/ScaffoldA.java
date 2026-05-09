package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;


@CheckData(
        name = "ScaffoldA",
        description = "GodBridge/KeepY scaffold analysis"
)
public final class ScaffoldA extends ScaffoldCheck {
    private static final long DRAG_CLICK_INTERVAL_MS = 50L;

    private long lastPlacementPacketAt;
    private double dragClick;

    public ScaffoldA(PlayerData player) {
        super(player);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock || place.position.y >= player.y) {
            return;
        }

        final BlockFace face = place.getFace();
        if (face == BlockFace.OTHER) return;

        if (now() - lastPlacementPacketAt > 500) {
            buffer = 0;
        }

        boolean keepY = face != BlockFace.UP && face != BlockFace.DOWN;
        if (keepY && getInferredInput(player).getZ() < 0) {
            if (!player.isSneaking && dragClick < 2) {
                buffer++;
            } else {
                buffer = Math.max(0, buffer - 1);
            }
        } else {
            buffer = 0;
        }

        if (buffer >= 5) {
            if (flagAndAlert("dc=" + dragClick + "\nb=" + buffer + "\nface=" + face) && shouldCancel()) {
                startCancelWindow();
                player.mitigateDamage();
                place.resync();
                buffer = 1;
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            if ((now() - lastPlacementPacketAt) > DRAG_CLICK_INTERVAL_MS) {
                dragClick = Math.max(0.0, dragClick - 1.0);
            } else {
                dragClick = Math.min(20.0, dragClick + 1.0);
            }
            lastPlacementPacketAt = now();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
    }
}
