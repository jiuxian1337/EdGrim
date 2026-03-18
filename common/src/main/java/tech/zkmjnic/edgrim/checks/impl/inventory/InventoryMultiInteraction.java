package tech.zkmjnic.edgrim.checks.impl.inventory;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

/**
 * Ported from AntiCheatAddition InventoryMultiInteraction.
 *
 * Detects too-fast successive inventory interactions.
 * Bukkit-side InventoryAction/slot-distance logic doesn't exist here, so this is a packet-level approximation
 * based on CLICK_WINDOW click type, slot distance, and time between clicks.
 */
@CheckData(
        name = "InventoryMultiInteraction",
        configName = "InventoryMultiInteraction",
        alternativeName = "AC",
        experimental = true,
        decay = 0.25,
        setback = -1,
        description = "Moved items too quickly in inventory"
)
public final class InventoryMultiInteraction extends Check implements PacketCheck {

    private boolean enabled = true;
    private double minTps = 17.0;
    private int maxPingMs = 350;
    private int cancelVl = 25;
    private boolean cancelPackets = false;

    private long lastClickMs;
    private int lastSlot = Integer.MIN_VALUE;

    public InventoryMultiInteraction(final EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(final ConfigManager config) {
        enabled = config.getBooleanElse("InventoryMultiInteraction.enabled", true);
        minTps = config.getDoubleElse("InventoryMultiInteraction.min-tps", 17.0);
        maxPingMs = config.getIntElse("InventoryMultiInteraction.max-ping", 350);
        cancelVl = config.getIntElse("InventoryMultiInteraction.cancelvl", 25);
        cancelPackets = config.getBooleanElse("InventoryMultiInteraction.cancel", false);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!isEnabled() || !enabled) {
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) {
            return;
        }

        // Only makes sense while a container window is open.
        final boolean containerOpen = player.inventory != null
                && player.inventory.menu != null
                && player.inventory.inventory != null
                && player.inventory.menu != player.inventory.inventory;
        if (!containerOpen) {
            return;
        }

        if (player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE
                || player.gamemode == com.github.retrooper.packetevents.protocol.player.GameMode.SPECTATOR) {
            return;
        }

        if (minTps > 0.0) {
            final double tps = EdGrimAPI.INSTANCE.getPlatformServer().getTPS();
            if (tps > 0.0 && tps < minTps) {
                return;
            }
        }

        final int ping = player.getTransactionPing();
        if (maxPingMs > 0 && ping > maxPingMs) {
            return;
        }

        final WrapperPlayClientClickWindow click = new WrapperPlayClientClickWindow(event);
        final int slot = click.getSlot();
        if (slot < 0 || slot == -999) {
            return;
        }

        // Spam clicking on the same slot is noisy and not very useful.
        if (slot == lastSlot) {
            lastClickMs = System.currentTimeMillis();
            return;
        }

        final long now = System.currentTimeMillis();
        final long deltaMs = now - lastClickMs;

        final int dist = lastSlot == Integer.MIN_VALUE ? 999 : Math.abs(slot - lastSlot);
        final boolean smallDistance = dist < 4;

        final WrapperPlayClientClickWindow.WindowClickType clickType = click.getWindowClickType();
        int addedVl = 6;
        int enforcedTicks;

        // Ported mapping from Bukkit InventoryAction to packet click types.
        // This isn't 1:1, but keeps the same idea: far-apart clicks need more time.
        switch (clickType) {
            case SWAP -> {
                addedVl = 1;
                enforcedTicks = 1;
                if (smallDistance) {
                    lastClickMs = now;
                    lastSlot = slot;
                    return;
                }
            }
            case PICKUP, PICKUP_ALL -> {
                addedVl = 8;
                enforcedTicks = smallDistance ? 1 : 5;
            }
            case QUICK_MOVE -> {
                enforcedTicks = smallDistance ? 1 : 2;
            }
            case THROW, CLONE -> {
                enforcedTicks = 4;
            }
            default -> {
                enforcedTicks = 0;
            }
        }

        final long thresholdMs = 25L + enforcedTicks * 50L;
        if (enforcedTicks > 0 && deltaMs >= 0L && deltaMs < thresholdMs) {
            final String verbose = "type=" + clickType
                    + " dt=" + deltaMs + "ms"
                    + " thr=" + thresholdMs + "ms"
                    + " dist=" + dist
                    + " ping=" + ping;

            // We don't have a clean Bukkit sync method here. Only cancel if explicitly enabled.
            final double oldVl = violations;
            if (flagAndAlert(verbose) && shouldModifyPackets()) {
                // Emulate AntiCheatAddition added-vl by adjusting our VL.
                // flag() already added +1, so set it to oldVl + (addedVl/10).
                violations = oldVl + (addedVl / 10.0);

                if (cancelPackets && violations >= cancelVl) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        } else {
            reward();
        }

        lastClickMs = now;
        lastSlot = slot;
    }
}
