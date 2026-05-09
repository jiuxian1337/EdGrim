package tech.zkmjnic.edgrim.checks.impl.badpackets;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "BadPacketsD", description = "Impossible pitch")
public class BadPacketsD extends Check implements PacketCheck {
    public BadPacketsD(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.packetStateData.lastPacketWasTeleport) return;

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            final float pitch = new WrapperPlayClientPlayerFlying(event).getLocation().getPitch();
            if (pitch > 90 || pitch < -90) {
                // Ban.
                if (flagAndAlert("pitch=" + pitch)) {
                    if (shouldModifyPackets()) {
                        // prevent other checks from using an invalid pitch
                        if (player.yRot > 90) player.yRot = 90;
                        if (player.yRot < -90) player.yRot = -90;

                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
            }
        }
    }
}
