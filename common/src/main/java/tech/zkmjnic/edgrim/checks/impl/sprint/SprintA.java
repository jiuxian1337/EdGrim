package tech.zkmjnic.edgrim.checks.impl.sprint;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "SprintA", description = "Sprinting with too low hunger", setback = 0)
public class SprintA extends Check implements PacketCheck {

    public SprintA(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            // Players can sprint if they're able to fly (MCP)
            if (player.canFly) return;

            if (player.food < 6.0F && player.isSprinting) {
                if (flagAndAlert("hunger=" + player.food)) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    if (shouldSetback()) {
                        player.getSetbackTeleportUtil().executeNonSimulatingSetback();
                    }
                }
            } else {
                reward();
            }
        }
    }
}
