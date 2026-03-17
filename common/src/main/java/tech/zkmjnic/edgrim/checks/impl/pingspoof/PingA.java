package tech.zkmjnic.edgrim.checks.impl.pingspoof;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

/**
 * Ported from Karhu PingSpoof PingA.
 *
 * Compares async keepalive ping (platform) against sync transaction ping.
 * Flags sustained cases where keepalive is much higher than transaction ping.
 */
@CheckData(
        name = "PingA",
        experimental = true,
        description = "Detects spoofed keepalive ping vs transaction ping"
)
public final class PingA extends Check implements PacketCheck {
    private boolean heartbeatReceived;
    private boolean suspicious;
    private double buffer;

    private long suspiciousDiffMs = 150L;
    private long allowedKeepaliveOverTransMs = 200L;
    private double bufferThreshold = 6.0;
    private double bufferDecay = 0.25;

    public PingA(final EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(final ConfigManager config) {
        suspiciousDiffMs = clamp(config.getLongElse("PingA.suspicious-diff-ms", 150L), 0L, 2000L);
        allowedKeepaliveOverTransMs = clamp(config.getLongElse("PingA.allowed-keepalive-over-transaction-ms", 200L), 0L, 5000L);
        bufferThreshold = clamp(config.getDoubleElse("PingA.buffer-threshold", 6.0), 1.0, 50.0);
        bufferDecay = clamp(config.getDoubleElse("PingA.buffer-decay", 0.25), 0.0, 5.0);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        // Heartbeat: a valid transaction response received (sync ping).
        if (isTransaction(type) && player.packetStateData.lastTransactionPacketWasValid) {
            final int keepAlivePing = player.getKeepAlivePing();
            if (keepAlivePing < 0) {
                heartbeatReceived = false;
                suspicious = false;
                return;
            }

            final int transPing = player.getTransactionPing();
            suspicious = Math.abs(keepAlivePing - transPing) > suspiciousDiffMs;
            heartbeatReceived = true;
            return;
        }

        // Evaluate on next movement/tick packet after the heartbeat.
        if (heartbeatReceived && isTickPacketIncludingNonMovement(type)) {
            final int keepAlivePing = player.getKeepAlivePing();
            final int transPing = player.getTransactionPing();
            if (keepAlivePing < 0) {
                heartbeatReceived = false;
                suspicious = false;
                return;
            }

            if (!suspicious || keepAlivePing <= transPing + allowedKeepaliveOverTransMs) {
                buffer = Math.max(0.0, buffer - bufferDecay);
            } else {
                buffer += 1.0;
                if (buffer >= bufferThreshold) {
                    final int diff = Math.abs(keepAlivePing - transPing);
                    flagAndAlert("diff=" + diff + "ms trans=" + transPing + "ms keepalive=" + keepAlivePing + "ms");
                    buffer = Math.max(0.0, bufferThreshold - 1.0);
                }
            }

            heartbeatReceived = false;
        }
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
