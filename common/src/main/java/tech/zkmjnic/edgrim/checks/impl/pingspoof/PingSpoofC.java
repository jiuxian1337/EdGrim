package tech.zkmjnic.edgrim.checks.impl.pingspoof;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.math.LatencyHistogram;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.*;

@CheckData(name = "PingSpoofC", description = "statistical latency anomaly (histogram + backtrack VL)", decay = 0.05)
public final class PingSpoofC extends Check implements PacketCheck {
    private final LatencyHistogram histogram = new LatencyHistogram();

    // Suspicious latency tracking (port of Intave FeedbackAnalysisMeta.suspiciousLatencies)
    private final List<SuspiciousLatency> suspiciousLatencies = new ArrayList<>(50);

    // Backtrack VL (port of Intave ViolationMetadata.backtrackVL)
    private long lastBacktrackVLChange;
    private long lastBacktrackHitCancel;

    // Short-term rolling average (for deviation check)
    private long shortSum, shortCount;

    // Frequency mismatch tracking
    private long natOccurrences, devOccurrences;
    private long lastFreqMismatchReport;
    private String lastFreqMismatchMessage = "";
    private long lastDeviationRecheck;
    private int cancelVL;

    public PingSpoofC(PlayerData player) {
        super(player);
    }

    @Override
    public void onReload(ConfigManager config) {
        super.onReload(config);
        this.cancelVL = config.getIntElse(getConfigName() + ".cancelVL", 5);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
            if (player.gamemode == GameMode.CREATIVE || player.gamemode == GameMode.SPECTATOR || player.inVehicle())
                return;

            int transPing = player.getTransactionPing();
            if (transPing <= 0) return;

            long generalAvg = shortCount > 0 ? shortSum / shortCount : transPing;
            boolean deviating = Math.abs(transPing - generalAvg) > 100;

            if (deviating) devOccurrences++;
            else natOccurrences++;

            // Recheck deviations every 4 seconds (Intave pattern)
            if (System.currentTimeMillis() - lastDeviationRecheck > 4000) {
                lastDeviationRecheck = System.currentTimeMillis();
                recheckDeviations();
            }

            // Protection 4 from Intave:
            // If near-entity transaction delay > 100ms AND recently attacked,
            // check if this latency is statistically improbable
            if (transPing > 100 && player.actionManager.hasAttackedSince(1000)) {
                double probability = histogram.biasedProbabilityOf(transPing, 300);
                // Intave threshold: 1 in ~150,000 (0.0000033976731)
                if (probability < 0.00001) {
                    suspiciousLatencies.add(new SuspiciousLatency(transPing));
                }
            }

            // Clean old suspicious latencies (>3 seconds old)
            suspiciousLatencies.removeIf(s -> System.currentTimeMillis() - s.time > 3000);

            // Intave: Check if there are suspicious latencies
            if (!suspiciousLatencies.isEmpty()) {
                suspiciousLatencies.sort(Comparator.comparingLong(SuspiciousLatency::latency).reversed());
                SuspiciousLatency mostSuspicious = suspiciousLatencies.get(0);

                double mean = histogram.mean();
                double stdDev = histogram.biasedStdDev(300);
                double zScore = (mostSuspicious.latency() - mean) / stdDev;
                boolean frequencyMismatch = recentlyDetectedForFreqMisrep();

                double addedVL = (zScore > 4.5 ? 1.0 : 0.5);
                if (frequencyMismatch) addedVL += 0.5;

                buffer = Math.min(buffer + addedVL, 3);
                if (buffer > 3) {
                    if (flagAndAlert(
                            "latency=" + mostSuspicious.latency() + "ms z=" + String.format("%.2f", zScore) +
                                    " mean=" + (int) mean + "ms sd=" + (int) stdDev +
                                    (frequencyMismatch ? " freq:" + lastFreqMismatchMessage : "")
                    )) {
                        if (shouldCancel()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                        lastBacktrackHitCancel = System.currentTimeMillis();
                    }
                }
                lastBacktrackVLChange = System.currentTimeMillis();

                suspiciousLatencies.clear();
            }

            // Intave: decay backtrackVL after 7.5 seconds of inactivity
            if (System.currentTimeMillis() - lastBacktrackVLChange > 7500) {
                buffer = Math.max(0, buffer - 1);
                lastBacktrackVLChange = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - lastBacktrackHitCancel < 5000) {
                if (shouldCancel() && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                    player.mitigateDamage();
                }
            }

            return;
        }

        if (isFlyingPacket(event)) {
            int transPing = player.getTransactionPing();
            if (transPing <= 0) return;

            // Feed histogram with all transaction pings
            histogram.addLatency(transPing);

            // Update short-term average
            long capped = Math.min(transPing, 1000);
            shortSum += capped;
            shortCount++;
            if (shortCount > 500) {
                shortSum /= 2;
                shortCount /= 2;
            }
        }
    }

    private void recheckDeviations() {
        long total = natOccurrences + devOccurrences;
        if (total > 0 && devOccurrences > 10 && natOccurrences > 20) {
            double devRate = (double) devOccurrences / total;
            double natRate = (double) natOccurrences / total;
            // Intave: deviating/natural rate > 3x
            if (devRate > natRate * 3) {
                lastFreqMismatchReport = System.currentTimeMillis();
                lastFreqMismatchMessage = "dev=" + String.format("%.1f", devRate * 100) +
                        "% nat=" + String.format("%.1f", natRate * 100) + "%";
            }
        }
        // Downscale to prevent overflow (Intave pattern)
        if (total > 10000) {
            natOccurrences = (long) (natOccurrences / 1.3333);
            devOccurrences = (long) (devOccurrences / 1.3333);
        }
    }

    private boolean recentlyDetectedForFreqMisrep() {
        return System.currentTimeMillis() - lastFreqMismatchReport < 8000;
    }

    private boolean shouldCancel() {
        return violations >= cancelVL;
    }

    private boolean isFlyingPacket(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private static class SuspiciousLatency {
        private final long latency;
        private final long time;

        SuspiciousLatency(long latency) {
            this.latency = latency;
            this.time = System.currentTimeMillis();
        }

        long latency() { return latency; }
    }
}
