package tech.zkmjnic.edgrim.checks.impl.pingspoof;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "PingSpoofB", description = "combat vs idle transaction ping discrepancy", decay = 0.05)
public final class PingSpoofB extends Check implements PacketCheck {
    // Rolling averages for transaction ping
    private static final int SAMPLE_SIZE = 256;

    private long generalSum, combatSum;
    private long generalCount, combatCount;
    private long shortSum, shortCount;
    private long lastShortReset;
    private int cancelVL;

    public PingSpoofB(PlayerData player) {
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

            // Add to combat average
            addToCombat(transPing);

            // Protection 3 from Intave: combat vs general latency discrepancy
            long generalAvg = generalCount > 0 ? generalSum / generalCount : transPing;
            long combatAvg = combatCount > 0 ? combatSum / combatCount : transPing;
            long shortAvg = shortCount > 0 ? shortSum / shortCount : transPing;

            long discrepancy = combatAvg - generalAvg;

            // Intave: entityLatencyDiscrepancy > 50 && ticksOverLimit >= 0
            // Intave: Math.abs(general - combat) > 75
            double vlAdded = 0;

            if (discrepancy > 50 && combatCount > 50) {
                vlAdded += 0.5;
                if (discrepancy > 75) vlAdded += 0.5;
                if (discrepancy > 150) vlAdded += 0.5;
            }

            // Short-term vs long-term divergence (Protection 3b: shortTransactionPingAverage vs transactionPingAverage)
            long longAvg = Math.max(generalAvg, combatAvg);
            if (shortAvg > longAvg + 50 && shortCount > 10 && generalCount > 50) {
                vlAdded += 0.5;
            }

            // Transaction ping vs keepalive ping check (supplement to existing PingSpoofA)
            int keepAlivePing = player.getKeepAlivePing();
            if (keepAlivePing > 0 && transPing > keepAlivePing + 200) {
                vlAdded += 0.5;
            }

            if (vlAdded > 0) {
                buffer += vlAdded;
                if (buffer >= 1.0 && flagAndAlert(
                        "disc=" + discrepancy + "ms combat=" + combatAvg +
                                "ms general=" + generalAvg + "ms short=" + shortAvg + "ms"
                ) && shouldCancel()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                buffer = Math.max(0, buffer * 0.95);
            }

            return;
        }

        if (isFlyingPacket(event)) {
            int transPing = player.getTransactionPing();
            if (transPing <= 0) return;

            // Add to general average
            addToGeneral(transPing);

            // Reset short-term average every 4 seconds (Intave pattern)
            if (System.currentTimeMillis() - lastShortReset > 4000) {
                shortSum = 0;
                shortCount = 0;
                lastShortReset = System.currentTimeMillis();
            }
            addToShort(transPing);
        }
    }

    private void addToGeneral(long ping) {
        ping = Math.min(ping, 1000);
        generalSum += ping;
        generalCount++;
        if (generalCount > SAMPLE_SIZE) {
            generalSum /= 2;
            generalCount /= 2;
        }
    }

    private void addToCombat(long ping) {
        ping = Math.min(ping, 1000);
        combatSum += ping;
        combatCount++;
        if (combatCount > SAMPLE_SIZE / 2) {
            combatSum /= 2;
            combatCount /= 2;
        }
    }

    private void addToShort(long ping) {
        ping = Math.min(ping, 1000);
        shortSum += ping;
        shortCount++;
        if (shortCount > SAMPLE_SIZE / 4) {
            shortSum /= 2;
            shortCount /= 2;
        }
    }

    private boolean isFlyingPacket(PacketReceiveEvent event) {
        return event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private boolean shouldCancel() {
        return violations >= cancelVL;
    }
}
