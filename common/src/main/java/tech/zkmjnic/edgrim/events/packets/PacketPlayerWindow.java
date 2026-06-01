package tech.zkmjnic.edgrim.events.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.player.PlayerData;

public class PacketPlayerWindow extends PacketListenerAbstract {

    public PacketPlayerWindow() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) && !event.isCancelled()) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            if (player.hasInventoryOpen && isNearNetherPortal(player)) {
                handleInventoryClose(player, "NETHER_PORTAL");
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            WrapperPlayClientClientStatus wrapper = new WrapperPlayClientClientStatus(event);
            if (wrapper.getAction() == Action.OPEN_INVENTORY_ACHIEVEMENT) {
                PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
                if (player == null) return;
                handleInventoryOpen(player);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                    && player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) {
                handleInventoryOpen(player);
            }
            if (player.getClientVersion().isNewerThan(ClientVersion.V_1_8)) {
                handleInventoryOpen(player);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            handleInventoryClose(player, "NOT_DESYNCED");
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryClose(player, "NOT_DESYNCED"));
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow wrapper = new WrapperPlayServerOpenWindow(event);
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.sendTransaction();
            String legacyType = wrapper.getLegacyType();
            int modernType = wrapper.getType();
            String desyncStatus = getContainerDesyncStatus(player, legacyType, modernType);
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> {
                        if ("NOT_DESYNCED".equals(desyncStatus)) {
                            handleInventoryOpen(player);
                        } else {
                            handleInventoryClose(player, desyncStatus);
                        }
                    });
        } else if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryOpen(player));
        } else if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            PlayerData player = EdGrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(),
                    () -> handleInventoryClose(player, "NOT_DESYNCED"));
        }
    }

    private void handleInventoryOpen(PlayerData player) {
        if (!player.hasInventoryOpen) {
            player.lastInventoryOpen = System.currentTimeMillis();
        }
        player.hasInventoryOpen = true;
    }

    private void handleInventoryClose(PlayerData player, String desyncStatus) {
        player.hasInventoryOpen = false;
        player.inventoryDesyncStatus = desyncStatus;
    }

    private String getContainerDesyncStatus(PlayerData player, String legacyType, int modernType) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) &&
                ("minecraft:beacon".equals(legacyType) || modernType == 8)) {
            return player.inventoryDesyncStatus = "BEACON";
        }
        if (isNearNetherPortal(player)) {
            return player.inventoryDesyncStatus = "NETHER_PORTAL";
        }
        return player.inventoryDesyncStatus = "NOT_DESYNCED";
    }

    private boolean isNearNetherPortal(PlayerData player) {
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_12_1) &&
                player.pointThreeEstimator.isNearNetherPortal) {
            return !player.compensatedEntities.self.inVehicle() && player.compensatedEntities.self.passengers.isEmpty();
        }
        return false;
    }
}
