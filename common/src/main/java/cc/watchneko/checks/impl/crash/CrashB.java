package cc.watchneko.checks.impl.crash;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.PacketCheck;
import cc.watchneko.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "CrashB", description = "Sent creative mode inventory click packets while not in creative mode")
public class CrashB extends Check implements PacketCheck {
    public CrashB(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            if (player.gamemode != GameMode.CREATIVE) {
                event.setCancelled(true);
                player.onPacketCancel();
                flagAndAlert(); // Could be transaction split, no need to setback though
            }
        }
    }
}
