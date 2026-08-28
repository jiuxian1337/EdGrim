package cc.watchneko.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.ScaffoldCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;


@CheckData(
        name = "ScaffoldA",
        description = "GodBridge/KeepY scaffold analysis"
)
public final class ScaffoldA extends ScaffoldCheck {
    private static final long DRAG_CLICK_INTERVAL_MS = 50L;

    private long lastPlacementPacketAt;
    private double dragClick;
    private int lastYLevel;

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

        if ((now() - lastPlacementPacketAt > 500 && player.onGround) || now() - lastPlacementPacketAt > 2000 || place.position.y != lastYLevel) {
            buffer = 0;
        }

        boolean keepY = face != BlockFace.UP && face != BlockFace.DOWN;
        if (keepY) {
            if ((!player.isSneaking && dragClick < 2 && getInferredInput(player).getZ() < 0) || !player.onGround) {
                buffer++;
            } else {
                buffer = Math.max(0, buffer - 1);
            }
        } else {
            buffer = 0;
        }

        if (buffer >= 4) {
            if (flagAndAlert("dc=" + dragClick + "\nb=" + buffer + "\nface=" + face)) {
                if (shouldCancel()) {
                    cancel();
                    player.mitigateDamage();
                    place.resync();
                }
                buffer = 1;
            }
        }
        lastYLevel = place.position.y;
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
