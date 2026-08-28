package cc.watchneko.checks.impl.multiactions;

import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.BlockPlaceCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockPlace;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;

@CheckData(name = "MultiActionsG", description = "Attacking or using items while rowing a boat", experimental = true)
public class MultiActionsG extends BlockPlaceCheck {
    public MultiActionsG(PlayerData player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY && isCheckActive()
                && flagAndAlert("interact") && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && isCheckActive()
                && flagAndAlert("use") && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (isCheckActive() && flagAndAlert(place.getFace() == BlockFace.OTHER ? "use" : "place") && shouldModifyPackets() && shouldCancel()) {
            place.resync();
        }
    }

    public boolean isCheckActive() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) && !player.vehicleData.wasVehicleSwitch // one tick off?
                && player.inVehicle() && player.compensatedEntities.self.getRiding().type.isInstanceOf(EntityTypes.BOAT)
                && (player.vehicleData.nextVehicleForward != 0 || player.vehicleData.nextVehicleHorizontal != 0);
    }
}
