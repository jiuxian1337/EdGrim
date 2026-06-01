package tech.zkmjnic.edgrim.checks.impl.stuck;

import ac.grim.grimac.api.config.ConfigManager;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "StuckA")
public final class StuckA extends Check implements PacketCheck {

    private long lastC03;
    private int avgPing;

    public StuckA(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.onGround) return;
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            lastC03 = System.currentTimeMillis();
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            check(event);
        }
    }

    public void check(PacketReceiveEvent event) {
        int currentPing = (player.getTransactionPing() + player.getKeepAlivePing()) / 2;
        if (avgPing <= 0) {
            avgPing = currentPing;
        } else {
            long c03Delay = System.currentTimeMillis() - lastC03;
            if (lastC03 != 0 && c03Delay > avgPing + 100 && flagAndAlert("avg=" + avgPing + "\ncurrent=" + c03Delay)) {
                event.setCancelled(true);
                player.mitigateDamage();
            }
            avgPing = (avgPing + currentPing) / 2;
        }
    }

}
