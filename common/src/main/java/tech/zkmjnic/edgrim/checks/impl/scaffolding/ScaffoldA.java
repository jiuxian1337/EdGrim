package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.ScaffoldCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import tech.zkmjnic.edgrim.utils.anticheat.update.RotationUpdate;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(
        name = "ScaffoldA",
        configName = "ScaffoldA",
        decay = 0.86,
        description = "GodBridge/KeepY scaffold analysis"
)
public final class ScaffoldA extends ScaffoldCheck {
    private static final long DRAG_CLICK_INTERVAL_MS = 50L;
    private static final long RESET_AFTER_IDLE_MS = 1000L;

    private long lastPlacementPacketAt;
    private double dragClick;
    private double godBridgeBuffer;
    private double godBridgeStreak;
    private double placeSpeed;
    private int placeCounter;
    private int tickCounter;
    private int lastSneakTicks = 100;
    private int lastPlaceY = Integer.MIN_VALUE;
    private float pitch;

    private boolean godBridgeEnabled;
    private boolean cancelPlacements;

    public ScaffoldA(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.pitch = rotationUpdate.getTo().getPitch();
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock) {
            return;
        }

        if (cancelPlaceIfWindowActive(place)) {
            return;
        }

        if (!godBridgeEnabled) {
            return;
        }

        placeCounter++;
        updateGodBridgeBuffer(place);

        if (dragClick < 5.0 && godBridgeBuffer > 3.0) {
            final long sinceLastPlacement = now() - lastPlacementPacketAt;
            if (sinceLastPlacement > RESET_AFTER_IDLE_MS) {
                godBridgeBuffer = 0.0;
                godBridgeStreak = 0.0;
                return;
            }

            if (godBridgeStreak++ > 3.0
                    && flagAndAlert("(GodBridge/KeepY)\ndc= " + dragClick + "\nlc= " + sinceLastPlacement)
                    && shouldCancel()
                    && cancelPlacements) {
                startCancelWindow();
                if (shouldModifyPackets()) {
                    place.resync();
                }
            }
            return;
        }

        godBridgeStreak = Math.max(0.0, godBridgeStreak - (placeSpeed <= 0.6 ? 1.0 : 0.6));
    }

    private void updateGodBridgeBuffer(BlockPlace place) {
        final BlockFace face = place.getFace();
        if (face == BlockFace.OTHER || face == BlockFace.UP || face == BlockFace.DOWN) {
            return;
        }

        if (place.position.getY() == lastPlaceY) {
            if (lastSneakTicks > 5 && pitch >= 45.0F && dragClick < 2.0) {
                godBridgeBuffer = Math.min(5.0, godBridgeBuffer + 1.0);
            } else if (lastSneakTicks <= 5 || dragClick > 1.0) {
                godBridgeBuffer = 0.0;
            }
        } else {
            godBridgeBuffer = Math.max(0.0, godBridgeBuffer - 1.5);
        }

        lastPlaceY = place.position.getY();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isFlying(event.getPacketType())) {
            tickCounter++;
            if (tickCounter > 30) {
                placeSpeed = (double) placeCounter / tickCounter;
                tickCounter = 0;
                placeCounter = 0;
            }

            if (player.isSneaking) {
                lastSneakTicks = 0;
            } else {
                lastSneakTicks = Math.min(lastSneakTicks + 1, 100);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            final long now = now();
            if ((now - lastPlacementPacketAt) > DRAG_CLICK_INTERVAL_MS) {
                dragClick = Math.max(0.0, dragClick - 1.0);
            } else {
                dragClick = Math.min(20.0, dragClick + 1.0);
            }
            lastPlacementPacketAt = now;
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        godBridgeEnabled = config.getBooleanElse("ScaffoldA.check-low-cps-god-bridge.enable", true);
        cancelPlacements = config.getBooleanElse("ScaffoldA.check-low-cps-god-bridge.cancel", true);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
