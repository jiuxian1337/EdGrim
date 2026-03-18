package tech.zkmjnic.edgrim.checks.impl.inventory;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;

/**
 * Ported from AntiCheatAddition InventoryRotation.
 *
 * Packet-based approximation:
 * - Inventory open state uses CompensatedInventory for container windows
 * - Legacy player inventory open uses CLIENT_STATUS OPEN_INVENTORY_ACHIEVEMENT
 *
 * Flags rotation changes while an inventory is open.
 */
@CheckData(
        name = "InventoryRotation",
        configName = "InventoryRotation",
        alternativeName = "AC",
        experimental = true,
        decay = 0.25,
        setback = -1,
        description = "Rotated while having an inventory open"
)
public final class InventoryRotation extends Check implements PacketCheck {

    private boolean enabled = true;
    private double minTps = 17.0;
    private long minOpenMs = 1000L;
    private long legacyOpenTimeoutMs = 5000L;
    private float minRotationDelta = 0.0f;

    private long inventoryOpenUntilMs;
    private long lastInventoryOpenMs;
    private boolean lastContainerOpen;

    public InventoryRotation(final EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(final ConfigManager config) {
        enabled = config.getBooleanElse("InventoryRotation.enabled", true);
        minTps = config.getDoubleElse("InventoryRotation.min-tps", 17.0);
        minOpenMs = clamp(config.getLongElse("InventoryRotation.min-open-ms", 1000L), 0L, 60000L);
        legacyOpenTimeoutMs = clamp(config.getLongElse("InventoryRotation.legacy-open-timeout-ms", 5000L), 500L, 60000L);
        minRotationDelta = (float) config.getDoubleElse("InventoryRotation.min-rotation-delta", 0.0);
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

        if (!isTickPacket(type)) {
            return;
        }

        if (minTps > 0.0) {
            final double tps = EdGrimAPI.INSTANCE.getPlatformServer().getTPS();
            if (tps > 0.0 && tps < minTps) {
                return;
            }
        }

        // Authoritative container-open state.
        final boolean containerOpen = player.inventory != null
                && player.inventory.menu != null
                && player.inventory.inventory != null
                && player.inventory.menu != player.inventory.inventory;

        if (containerOpen && !lastContainerOpen) {
            lastInventoryOpenMs = now;
        }
        lastContainerOpen = containerOpen;

        final boolean legacyOpen = now < inventoryOpenUntilMs;
        final boolean inventoryOpen = containerOpen || legacyOpen;
        if (!inventoryOpen) {
            return;
        }

        // Match original intent: don't flag flight situations.
        if (player.canFly || player.isFlying || player.isGliding || player.inVehicle()) {
            return;
        }

        // Teleports/setbacks/desync windows.
        if (player.packetStateData.lastPacketWasTeleport
                || player.getSetbackTeleportUtil().isSendingSetback
                || player.getSetbackTeleportUtil().shouldBlockMovement()
                || !player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport) {
            return;
        }

        // Must have inventory open for at least one second.
        if (minOpenMs > 0 && now - lastInventoryOpenMs < minOpenMs) {
            return;
        }

        final float deltaYaw = Math.abs(player.yRot - player.lastYRot);
        final float deltaPitch = Math.abs(player.xRot - player.lastXRot);
        if (deltaYaw <= minRotationDelta && deltaPitch <= minRotationDelta) {
            return;
        }

        flagAndAlert(String.format("dy=%.3f dp=%.3f", deltaYaw, deltaPitch));
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
