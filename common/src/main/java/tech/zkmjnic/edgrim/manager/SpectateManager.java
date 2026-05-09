package tech.zkmjnic.edgrim.manager;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.init.ReloadableInitable;
import tech.zkmjnic.edgrim.manager.init.start.StartableInitable;
import tech.zkmjnic.edgrim.platform.api.player.PlatformPlayer;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.math.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpectateManager implements StartableInitable, ReloadableInitable {

    private final Map<UUID, PreviousState> spectatingPlayers = new ConcurrentHashMap<>();
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> allowedWorlds = ConcurrentHashMap.newKeySet();

    private boolean checkWorld = false;

    @Override
    public void start() {
        reload();
    }

    @Override
    public void reload() {
        allowedWorlds.clear();
        allowedWorlds.addAll(EdGrimAPI.INSTANCE.getConfigManager().getConfig().getStringListElse("spectators.allowed-worlds", new ArrayList<>()));
        checkWorld = !(allowedWorlds.isEmpty() || new ArrayList<>(allowedWorlds).get(0).isEmpty());
    }

    public boolean isSpectating(UUID uuid) {
        return spectatingPlayers.containsKey(uuid);
    }

    public boolean shouldHidePlayer(PlayerData receiver, WrapperPlayServerPlayerInfo.PlayerData playerData) {
        return playerData.getUser() != null
                && playerData.getUser().getUUID() != null
                && shouldHidePlayer(receiver, playerData.getUser().getUUID());
    }

    public boolean shouldHidePlayer(PlayerData receiver, UUID uuid) {
        return !Objects.equals(uuid, receiver.uuid) // don't hide to yourself
                && (spectatingPlayers.containsKey(uuid) || hiddenPlayers.contains(uuid)) //hide if you are a spectator
                && !(receiver.uuid != null && (spectatingPlayers.containsKey(receiver.uuid) || hiddenPlayers.contains(receiver.uuid))) // don't hide to other spectators
                && (!checkWorld || (receiver.platformPlayer != null && allowedWorlds.contains(receiver.platformPlayer.getWorld().getName()))); // hide if you are in a specific world
    }

    public boolean enable(PlatformPlayer platformPlayer) {
        if (spectatingPlayers.containsKey(platformPlayer.getUniqueId())) return false;
        spectatingPlayers.put(platformPlayer.getUniqueId(), new PreviousState(platformPlayer.getGameMode(), platformPlayer.getLocation()));
        return true;
    }

    public void onLogin(UUID uuid) {
        hiddenPlayers.add(uuid);
    }

    public void onQuit(UUID uuid) {
        hiddenPlayers.remove(uuid);
        handlePlayerStopSpectating(uuid);
    }

    // only call this synchronously
    public void disable(@NonNull PlatformPlayer platformPlayer, boolean teleportBack) {
        PreviousState previousState = spectatingPlayers.get(platformPlayer.getUniqueId());
        if (previousState != null) {
            if (teleportBack && previousState.location.isWorldLoaded()) {
                platformPlayer.teleportAsync(previousState.location).thenAccept(bool -> {
                    if (bool) {
                        onDisable(previousState, platformPlayer);
                    } else {
                        platformPlayer.sendMessage(Component.text("Teleport failed, please try again.", NamedTextColor.RED));
                    }
                });
            } else {
                onDisable(previousState, platformPlayer);
            }
        }
    }

    private void onDisable(PreviousState previousState, PlatformPlayer platformPlayer) {
        platformPlayer.setGameMode(previousState.gameMode);
        handlePlayerStopSpectating(platformPlayer.getUniqueId());
    }

    public void handlePlayerStopSpectating(UUID uuid) {
        spectatingPlayers.remove(uuid);
    }

    private record PreviousState(GameMode gameMode, Location location) {
    }
}
