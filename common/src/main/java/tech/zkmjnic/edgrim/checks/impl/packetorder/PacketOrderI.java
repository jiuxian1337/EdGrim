package tech.zkmjnic.edgrim.checks.impl.packetorder;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;
import tech.zkmjnic.edgrim.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderI")
public class PacketOrderI extends Check implements PostPredictionCheck {
    public PacketOrderI(final EdGrimPlayer player) {
        super(player);
    }


    private boolean setback;
    private boolean digging; // for placing
    private final ArrayDeque<String> flags = new ArrayDeque<>();

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                if (player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
                    String verbose = "type=attack, rightClicking=" + player.packetOrderProcessor.isRightClicking()
                            + ", picking=" + player.packetOrderProcessor.isPicking()
                            + ", releasing=" + player.packetOrderProcessor.isReleasing()
                            + ", digging=" + player.packetOrderProcessor.isDigging();
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert(verbose) && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        flags.add(verbose);
                    }
                }
            } else if (player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
                String verbose = "type=interact, releasing=" + player.packetOrderProcessor.isReleasing() + ", digging=" + player.packetOrderProcessor.isDigging();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (player.packetOrderProcessor.isReleasing() || digging) {
                String verbose = "type=place/use, releasing=" + player.packetOrderProcessor.isReleasing() + ", digging=" + digging;
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(verbose) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(verbose);
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case RELEASE_USE_ITEM:
                    if (player.packetOrderProcessor.isAttacking() || player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isDigging()) {
                        String verbose = "type=release, attacking=" + player.packetOrderProcessor.isAttacking()
                                + ", rightClicking=" + player.packetOrderProcessor.isRightClicking()
                                + ", picking=" + player.packetOrderProcessor.isPicking()
                                + ", digging=" + player.packetOrderProcessor.isDigging();
                        if (!player.canSkipTicks()) {
                            if (flagAndAlert(verbose)) {
                                setback = true;
                            }
                        } else {
                            flags.add(verbose);
                            setback = true;
                        }
                    }
                    break;
                case START_DIGGING:
                    double damage = BlockBreakSpeed.getBlockDamage(player, player.compensatedWorld.getBlock(packet.getBlockPosition()));
                    if (damage >= 1 || damage <= 0 && player.gamemode == GameMode.CREATIVE) {
                        return;
                    }
            }
        }

        if (player.gamemode == GameMode.SPECTATOR || isTickPacket(event.getPacketType())) {
            digging = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setbackIfAboveSetbackVL();
                setback = false;
            }
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (String verbose : flags) {
                if (flagAndAlert(verbose) && setback) {
                    setbackIfAboveSetbackVL();
                    setback = false;
                }
            }
        }

        flags.clear();
        setback = false;
    }
}
