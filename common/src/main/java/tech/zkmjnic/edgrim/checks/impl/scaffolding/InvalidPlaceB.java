package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockPlaceCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceB", description = "Sent impossible block face id")
public class InvalidPlaceB extends BlockPlaceCheck {
    public InvalidPlaceB(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.getFaceId() == 255 && PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        if (place.getFaceId() < 0 || place.getFaceId() > 5) {
            // ban
            if (flagAndAlert("direction=" + place.getFaceId()) && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
