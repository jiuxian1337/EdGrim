package tech.zkmjnic.edgrim.utils.team;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.data.packetentity.PacketEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

// Reminder: Entities use UUIDs, players use name, for setting teams.
public class TeamHandler extends Check implements PacketCheck {
    private final Map<String, EntityTeam> entityTeams = new Object2ObjectOpenHashMap<>();
    private final Map<String, EntityTeam> entityToTeam = new Object2ObjectOpenHashMap<>();
    private @Getter
    @Setter
    @Nullable EntityTeam playerTeam = null;

    public TeamHandler(PlayerData player) {
        super(player);
    }

    public void addEntityToTeam(String entityTeamRepresentation, EntityTeam team) {
        entityToTeam.put(entityTeamRepresentation, team);
    }

    public void removeEntityFromTeam(String entityTeamRepresentation) {
        entityToTeam.remove(entityTeamRepresentation);
    }

    public EntityTeam getEntityTeam(PacketEntity entity) {
        // TODO in what cases is UUID null in 1.9+?
        final UUID uuid = entity.getUuid();
        return uuid == null ? null : entityToTeam.get(uuid.toString());
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.TEAMS) {
            TeamPacketData teams = readTeamPacket(event);
            final String teamName = teams.teamName();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                EntityTeam entityTeam = switch (teams.teamMode()) {
                    case CREATE -> {
                        var newTeam = new EntityTeam(player, teamName);
                        entityTeams.put(teamName, newTeam);
                        yield newTeam;
                    }
                    case REMOVE -> entityTeams.remove(teamName);
                    default -> entityTeams.get(teamName);
                };

                if (entityTeam != null) {
                    entityTeam.update(teams.teamMode(), teams.collisionRule(), teams.players());
                }
            });
        }
    }

    private TeamPacketData readTeamPacket(PacketSendEvent event) {
        PacketWrapper<?> wrapper = new PacketWrapper<>(event, false);
        int teamNameLimit = wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_18) ? 32767 : 16;
        String teamName = wrapper.readString(teamNameLimit);
        WrapperPlayServerTeams.TeamMode teamMode = WrapperPlayServerTeams.TeamMode.values()[wrapper.readByte()];
        WrapperPlayServerTeams.CollisionRule collisionRule = null;

        if (teamMode == WrapperPlayServerTeams.TeamMode.CREATE || teamMode == WrapperPlayServerTeams.TeamMode.UPDATE) {
            collisionRule = readCollisionRule(wrapper);
        }

        Collection<String> players = new ArrayList<>();
        if (teamMode == WrapperPlayServerTeams.TeamMode.CREATE
                || teamMode == WrapperPlayServerTeams.TeamMode.ADD_ENTITIES
                || teamMode == WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES) {
            int size = wrapper.getServerVersion().isOlderThanOrEquals(ServerVersion.V_1_7_10) ? wrapper.readShort() : wrapper.readVarInt();
            for (int i = 0; i < size; i++) {
                players.add(wrapper.readString(40));
            }
        }

        return new TeamPacketData(teamName, teamMode, collisionRule, players);
    }

    private WrapperPlayServerTeams.CollisionRule readCollisionRule(PacketWrapper<?> wrapper) {
        if (wrapper.getServerVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2)) {
            wrapper.readString(32); // display name
            wrapper.readString(16); // prefix
            wrapper.readString(16); // suffix
            wrapper.readByte(); // options

            if (wrapper.getServerVersion().isOlderThanOrEquals(ServerVersion.V_1_7_10)) {
                return WrapperPlayServerTeams.CollisionRule.ALWAYS;
            }

            wrapper.readString(32); // name tag visibility
            WrapperPlayServerTeams.CollisionRule collisionRule = WrapperPlayServerTeams.CollisionRule.ALWAYS;
            if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
                collisionRule = WrapperPlayServerTeams.CollisionRule.fromID(wrapper.readString(32));
            }

            if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
                wrapper.readVarInt(); // color
            } else {
                wrapper.readByte(); // color
            }
            return collisionRule == null ? WrapperPlayServerTeams.CollisionRule.ALWAYS : collisionRule;
        }

        skipComponent(wrapper); // display name
        wrapper.readByte(); // options

        WrapperPlayServerTeams.CollisionRule collisionRule;
        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            wrapper.readEnum(WrapperPlayServerTeams.NameTagVisibility.class);
            collisionRule = wrapper.readEnum(WrapperPlayServerTeams.CollisionRule.class);
        } else {
            wrapper.readString(40); // name tag visibility
            collisionRule = WrapperPlayServerTeams.CollisionRule.fromID(wrapper.readString(40));
        }

        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) {
            wrapper.readVarInt(); // color
        } else {
            wrapper.readByte(); // color
        }

        skipComponent(wrapper); // prefix
        skipComponent(wrapper); // suffix
        return collisionRule == null ? WrapperPlayServerTeams.CollisionRule.ALWAYS : collisionRule;
    }

    private void skipComponent(PacketWrapper<?> wrapper) {
        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_3)) {
            wrapper.readNBTRaw();
        } else {
            wrapper.readString(wrapper.getMaxMessageLength());
        }
    }

    private record TeamPacketData(
            String teamName,
            WrapperPlayServerTeams.TeamMode teamMode,
            @Nullable WrapperPlayServerTeams.CollisionRule collisionRule,
            Collection<String> players
    ) {
    }
}
