package tech.zkmjnic.edgrim.checks.impl.pingspoof;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(
        name = "PingA",
        description = "Detects spoofed keepalive ping vs transaction ping"
)
public final class PingA extends Check implements PacketCheck {
    private boolean kReceived;
    private boolean suspicious;

    public PingA(final PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
            this.suspicious = Math.abs(player.getKeepAlivePing() - player.getTransactionPing()) > 150L;
            this.kReceived = true;
            return;
        }

        if (isFlyingPacket(event) && this.kReceived) {
            if (!this.suspicious || player.getKeepAlivePing() <= player.getTransactionPing() + 200L) {
                this.violations = Math.max(this.violations - 0.25, 0.0);
            } else if (++this.violations >= 6.0) {
                if (flagAndAlert("diff: " + Math.abs(player.getKeepAlivePing() - player.getTransactionPing()) +
                        "\nTransactionPing: " + player.getTransactionPing() +
                        "\nKeepAlivePing: " + player.getKeepAlivePing())) {
                    player.mitigateDamage();
                }
            }

            this.kReceived = false;
        }
    }

    private boolean isFlyingPacket(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }
}
