package tech.zkmjnic.edgrim.checks.impl.crash;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockPlaceCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

@CheckData(name = "CrashG", description = "Sent negative sequence id")
public class CrashG extends BlockPlaceCheck {

    public CrashG(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isSupportedVersion()) {
            WrapperPlayClientUseItem use = new WrapperPlayClientUseItem(event);
            if (use.getSequence() < 0) {
                flagAndAlert();
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.sequence < 0 && isSupportedVersion()) {
            flagAndAlert();
            blockBreak.cancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (place.sequence < 0 && isSupportedVersion()) {
            flagAndAlert();
            place.resync();
        }
    }

    private boolean isSupportedVersion() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19);
    }

}
