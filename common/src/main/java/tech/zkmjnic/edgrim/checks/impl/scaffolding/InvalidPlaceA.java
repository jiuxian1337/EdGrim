package tech.zkmjnic.edgrim.checks.impl.scaffolding;

import com.github.retrooper.packetevents.util.Vector3f;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockPlaceCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockPlace;

@CheckData(name = "InvalidPlaceA", description = "Sent invalid cursor position")
public class InvalidPlaceA extends BlockPlaceCheck {
    public InvalidPlaceA(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        Vector3f cursor = place.cursor;
        if (cursor == null) return;
        if (!Float.isFinite(cursor.x) || !Float.isFinite(cursor.y) || !Float.isFinite(cursor.z)) {
            if (flagAndAlert() && shouldModifyPackets() && shouldCancel()) {
                place.resync();
            }
        }
    }
}
