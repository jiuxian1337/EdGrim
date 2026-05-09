package dev.jiuxian.edgrim;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RotationRecorder extends JavaPlugin implements Listener, PacketListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final int MAX_BUFFERED_UPDATES = 64;
    private static final int SAMPLE_RADIUS = 5;
    private static final int SAMPLE_SIZE = 11;
    private static final String RECORDED_DIR = "recorded";

    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private File recordedDirectory;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(false)
            .checkForUpdates(false)
            .bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
        Bukkit.getPluginManager().registerEvents(this, this);

        recordedDirectory = new File(getDataFolder(), RECORDED_DIR);
        if (!recordedDirectory.exists()) {
            recordedDirectory.mkdirs();
        }
        getLogger().info("EdGrimRotationRecorder has been enabled!");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("EdGrimRotationRecorder has been disabled!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        File playerDir = new File(recordedDirectory, sanitizeFileName(player.getName()));
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
        playerDataMap.put(player.getUniqueId(), new PlayerData(player.getUniqueId(), player.getName(), playerDir));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (event.getUser() == null) return;
            UUID uuid = event.getUser().getUUID();
            if (uuid == null) return;

            PlayerData data = playerDataMap.get(uuid);
            if (data == null) return;

            if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
                if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    data.attacksAwaitingCenter.addLast(data.rotationSequence + 1L);
                }
            } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

                // Grim's CheckManagerListener ONLY triggers RotationUpdate when hasLook is true.
                if (flying.hasRotationChanged() && flying.getLocation() != null) {
                    float yaw = flying.getLocation().getYaw();
                    float pitch = flying.getLocation().getPitch();

                    if (data.isFirstRotation) {
                        data.lastYaw = yaw;
                        data.lastPitch = pitch;
                        data.isFirstRotation = false;
                    }

                    float deltaYaw = yaw - data.lastYaw;
                    float deltaPitch = pitch - data.lastPitch;

                    data.lastYaw = yaw;
                    data.lastPitch = pitch;

                    data.rotationSequence++;
                    data.tickBuffer.addLast(new RotationFrame(data.rotationSequence, deltaYaw, deltaPitch));
                    while (data.tickBuffer.size() > MAX_BUFFERED_UPDATES) {
                        data.tickBuffer.removeFirst();
                    }

                    resolveAttackCenters(data);
                    drainPendingSamples(data);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resolveAttackCenters(PlayerData data) {
        while (!data.attacksAwaitingCenter.isEmpty()) {
            final long centerSequence = data.attacksAwaitingCenter.peekFirst();
            if (data.rotationSequence < centerSequence) {
                return;
            }
            data.attacksAwaitingCenter.removeFirst();
            data.pendingSamples.addLast(new PendingSample(centerSequence));
        }
    }

    private void drainPendingSamples(PlayerData data) {
        while (!data.pendingSamples.isEmpty()) {
            final PendingSample pending = data.pendingSamples.peekFirst();
            if (data.rotationSequence < pending.centerSequence + SAMPLE_RADIUS) {
                return;
            }

            data.pendingSamples.removeFirst();
            final RecordedAttackSample sample = extractSample(data, pending.centerSequence);
            if (sample == null) {
                continue;
            }

            writeRecordedSample(data, sample);
        }
    }

    private RecordedAttackSample extractSample(PlayerData data, long centerSequence) {
        if (data.tickBuffer.size() < SAMPLE_SIZE) {
            return null;
        }

        final List<RotationFrame> ticks = new ArrayList<>(data.tickBuffer);
        int centerIndex = -1;
        for (int i = 0; i < ticks.size(); i++) {
            if (ticks.get(i).sequence == centerSequence) {
                centerIndex = i;
                break;
            }
        }

        if (centerIndex < SAMPLE_RADIUS || centerIndex + SAMPLE_RADIUS >= ticks.size()) {
            return null;
        }

        final List<RotationFrame> window = ticks.subList(centerIndex - SAMPLE_RADIUS, centerIndex + SAMPLE_RADIUS + 1);
        final List<RecordedRotation> rotations = new ArrayList<>(window.size());
        for (RotationFrame tick : window) {
            rotations.add(new RecordedRotation(tick.deltaYaw, tick.deltaPitch));
        }
        return new RecordedAttackSample(rotations);
    }

    private void writeRecordedSample(PlayerData data, RecordedAttackSample sample) {
        try (FileWriter writer = new FileWriter(data.sessionFile, true)) {
            GSON.toJson(sample, writer);
            writer.write('\n');
        } catch (IOException exception) {
            getLogger().severe("Failed to write heuristic analysis sample: " + exception.getMessage());
        }
    }

    private String sanitizeFileName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "unknown";
        }
        return input.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
