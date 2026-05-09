package tech.zkmjnic.edgrim.checks.impl.badpackets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.data.HeadRotation;

import java.util.ArrayList;
import java.util.List;

@CheckData(name = "BadPacketsJ", description = "Rotation in use item packet did not match tick rotation")
public class BadPacketsJ extends Check implements PacketCheck {
    private final List<HeadRotation> rotations = new ArrayList<>();

    public BadPacketsJ(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.gamemode == GameMode.SPECTATOR) {
            rotations.clear();
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)
                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21)) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
            rotations.add(new HeadRotation(packet.getYaw(), packet.getPitch()));
        }

        if (isTickPacket(event.getPacketType())) {
            // due to tick skipping, the rotations sent could be last tick's
            boolean allowLast = player.canSkipTicks() && (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION);
            for (HeadRotation rotation : rotations) {
                if (rotation.getYaw() == player.xRot && rotation.getPitch() == player.yRot) {
                    allowLast = false;
                    continue;
                }

                if (rotation.getYaw() == player.lastXRot && rotation.getPitch() == player.lastYRot && allowLast) {
                    continue;
                }

                flagAndAlert();
            }

            rotations.clear();
        }
    }
}
