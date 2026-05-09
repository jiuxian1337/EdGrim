package tech.zkmjnic.edgrim.checks.impl.breaking;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockBreakCheck;
import tech.zkmjnic.edgrim.player.PlayerData;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;

@CheckData(name = "PositionBreakB", experimental = true)
public class PositionBreakB extends Check implements BlockBreakCheck {
    private final int releaseFace = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8) ? 0 : 255;
    private BlockFace lastFace;

    public PositionBreakB(PlayerData player) {
        super(player);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action == DiggingAction.START_DIGGING) {
            if (blockBreak.face == lastFace) {
                lastFace = null;
            }
        }

        if (lastFace != null) {
            flagAndAlert("lastFace=" + lastFace + ", action=" + blockBreak.action);
        }

        if (blockBreak.action == DiggingAction.CANCELLED_DIGGING) {
            lastFace = blockBreak.faceId == releaseFace ? null : blockBreak.face;
        }
    }
}
