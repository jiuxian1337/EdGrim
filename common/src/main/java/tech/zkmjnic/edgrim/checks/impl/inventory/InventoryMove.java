package tech.zkmjnic.edgrim.checks.impl.inventory;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.data.VectorData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;

/**
 * Ported from AntiCheatAddition InventoryMove.
 *
 * Packet-based approximation:
 * - We can reliably detect container windows (server OPEN_WINDOW tracked by CompensatedInventory).
 * - We can partially detect player inventory opening on legacy clients via CLIENT_STATUS OPEN_INVENTORY_ACHIEVEMENT.
 *
 * We then flag movement while inventory is considered open.
 */
@CheckData(
        name = "InventoryMove",
        description = "Moved while having an inventory open"
)
public final class InventoryMove extends Check implements PacketCheck {

    private static final double STANDING_STILL_THRESHOLD = 0.005;

    private boolean enabled = true;
    private long legacyOpenTimeoutMs = 5000L;
    private long ignoreAfterOpenMs = 300L;
    private long ignoreAfterTeleportMs = 500L;

    private long inventoryOpenUntilMs;
    private long lastInventoryOpenMs;

    public InventoryMove(final EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(final ConfigManager config) {
        enabled = config.getBooleanElse(getConfigName() + ".enabled", true);
        legacyOpenTimeoutMs = clamp(config.getLongElse(getConfigName() + ".legacy-open-timeout-ms", 5000L), 500L, 60000L);
        ignoreAfterOpenMs = clamp(config.getLongElse(getConfigName() + ".ignore-after-open-ms", 300L), 0L, 5000L);
        ignoreAfterTeleportMs = clamp(config.getLongElse(getConfigName() + ".ignore-after-teleport-ms", 500L), 0L, 5000L);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!isEnabled() || !enabled) {
            return;
        }

        final PacketTypeCommon type = event.getPacketType();
        final long now = System.currentTimeMillis();

        if (type == PacketType.Play.Client.CLIENT_STATUS) {
            final WrapperPlayClientClientStatus status = new WrapperPlayClientClientStatus(event);
            if (status.getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                lastInventoryOpenMs = now;
                inventoryOpenUntilMs = now + legacyOpenTimeoutMs;
            }
            return;
        }

        final boolean containerOpen = player.inventory != null
                && player.inventory.menu != null
                && player.inventory.inventory != null
                && player.inventory.menu != player.inventory.inventory;

        final boolean legacyOpen = now < inventoryOpenUntilMs;
        final boolean inventoryOpen = containerOpen || legacyOpen;
        if (!inventoryOpen) {
            return;
        }

        if (!isTickPacket(type)) {
            return;
        }

        if (player.getDeltaXZ() <= STANDING_STILL_THRESHOLD) {
            return;
        }

        if (player.inVehicle() || player.isFlying || player.isGliding) {
            return;
        }
        if (player.packetStateData.lastPacketWasTeleport || player.getSetbackTeleportUtil().isSendingSetback) {
            return;
        }
        if (!player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport) {
            return;
        }
        // Setback pending state is internal; use public shouldBlockMovement() as a safe approximation.
        if (ignoreAfterTeleportMs > 0 && player.getSetbackTeleportUtil().shouldBlockMovement()) {
            return;
        }
        if (player.wasTouchingWater || player.wasTouchingLava || player.slightlyTouchingWater || player.slightlyTouchingLava) {
            return;
        }
        if (now - lastInventoryOpenMs <= ignoreAfterOpenMs) {
            return;
        }
        if (player.predictedVelocity != null) {
            final VectorData.VectorType vt = player.predictedVelocity.vectorType;
            if (vt == VectorData.VectorType.Knockback
                    || vt == VectorData.VectorType.FirstBreadKnockback
                    || vt == VectorData.VectorType.Explosion
                    || vt == VectorData.VectorType.FirstBreadExplosion) {
                return;
            }
        }

        if (player.firstBreadKB != null || player.likelyKB != null || player.firstBreadExplosion != null || player.likelyExplosions != null) {
            return;
        }

        final String invKey = player.platformPlayer == null ? "null" : player.platformPlayer.getInventory().getOpenInventoryKey();
        final String verbose = "dxz=" + String.format("%.4f", player.getDeltaXZ())
                + " container=" + containerOpen
                + " inv=" + invKey;

        flagAndAlert(verbose);
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
