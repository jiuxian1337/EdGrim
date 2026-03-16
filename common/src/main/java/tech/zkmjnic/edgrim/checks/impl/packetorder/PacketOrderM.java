package tech.zkmjnic.edgrim.checks.impl.packetorder;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "PacketOrderM", experimental = true)
public class PacketOrderM extends Check implements PostPredictionCheck {
    public PacketOrderM(final EdGrimPlayer player) {
        super(player);
    }

    private int invalid;
    private boolean usingWithoutInteract, interacting;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                interacting = true;
                if (usingWithoutInteract) {
                    if (!player.canSkipTicks()) {
                        if (flagAndAlert() && shouldModifyPackets()) {
                            event.setCancelled(true);
                            player.onPacketCancel();
                        }
                    } else {
                        invalid++;
                    }
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM
                || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT
                && new WrapperPlayClientPlayerBlockPlacement(event).getFace() == BlockFace.OTHER) {
            if (!interacting) {
                usingWithoutInteract = true;
            }

            interacting = false;
        }

        if (player.gamemode == GameMode.SPECTATOR || isTickPacket(event.getPacketType())) {
            usingWithoutInteract = interacting = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) return;

        if (player.isTickingReliablyFor(3)) {
            for (; invalid >= 1; invalid--) {
                flagAndAlert();
            }
        }

        invalid = 0;
    }
}
