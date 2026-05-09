package tech.zkmjnic.edgrim.events.packets;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

import java.util.Objects;


public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> EdGrimAPI.INSTANCE.getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {
        // Player connected too soon, perhaps late bind is off
        // Don't kick everyone on reload
        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !EdGrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.contains(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        Object nativePlayerObject = Objects.requireNonNull(event.getPlayer());

        // This will never throw a NPE because code is run in OnUserConnect -> onPacketSend -> OnUserLogin order
        // And the user will be added to the map before the getPlayer() method call
        @NonNull PlatformPlayer platformPlayer = EdGrimAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(nativePlayerObject);

        if (EdGrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }
        if (platformPlayer.hasPermission("edgrim.alerts.enable-on-join") && platformPlayer.hasPermission("edgrim.alerts")) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleAlerts(platformPlayer, platformPlayer.hasPermission("edgrim.alerts.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("edgrim.verbose.enable-on-join") && platformPlayer.hasPermission("edgrim.verbose")) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleVerbose(platformPlayer, platformPlayer.hasPermission("edgrim.verbose.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("edgrim.brand.enable-on-join") && platformPlayer.hasPermission("edgrim.brand")) {
            EdGrimAPI.INSTANCE.getAlertManager().toggleBrands(platformPlayer, platformPlayer.hasPermission("edgrim.brand.enable-on-join.silent"));
        }
        if (platformPlayer.hasPermission("edgrim.spectate") && EdGrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("spectators.hide-regardless", false)) {
            EdGrimAPI.INSTANCE.getSpectateManager().onLogin(platformPlayer.getUniqueId());
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        EdGrimAPI.INSTANCE.getPlayerDataManager().onDisconnect(event.getUser());
    }
}
