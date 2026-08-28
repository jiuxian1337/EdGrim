package cc.watchneko.checks.impl.packetorder;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.PacketCheck;
import cc.watchneko.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "PacketOrderO", experimental = true)
public class PacketOrderO extends Check implements PacketCheck {
    private boolean flying;

    public PacketOrderO(final PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            flying = false;
        }

        if (isFlying(event.getPacketType()) && player.supportsEndTick() && !player.packetStateData.lastPacketWasTeleport) {
            flying = true;
            return;
        }

        if (flying && event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE
                && event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE
        ) {
            if (player.inVehicle() && event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
                if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                    return;
                }
            }

            flagAndAlert("type=" + event.getPacketType());
        }
    }
}
