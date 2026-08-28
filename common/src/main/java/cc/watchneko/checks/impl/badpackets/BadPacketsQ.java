package cc.watchneko.checks.impl.badpackets;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.PacketCheck;
import cc.watchneko.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ")
public class BadPacketsQ extends Check implements PacketCheck {
    public BadPacketsQ(final PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction wrapper = new WrapperPlayClientEntityAction(event);
            // you are able to send negative jump boost, how and why!?
            if (Math.abs(wrapper.getJumpBoost()) > 100
                    || wrapper.getEntityId() != player.entityID
                    || wrapper.getAction() != Action.START_JUMPING_WITH_HORSE && wrapper.getJumpBoost() != 0) {
                if (flagAndAlert("boost=" + wrapper.getJumpBoost() + ", action=" + wrapper.getAction() + ", entity=" + wrapper.getEntityId()) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }
}
