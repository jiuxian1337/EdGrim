package cc.watchneko.checks.impl.breaking;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.checks.type.BlockBreakCheck;
import cc.watchneko.player.PlayerData;
import cc.watchneko.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "NoSwingBreak", description = "Did not swing while breaking block", experimental = true)
public class NoSwingBreak extends Check implements BlockBreakCheck {
    private boolean sentAnimation;
    private boolean sentBreak;

    public NoSwingBreak(PlayerData playerData) {
        super(playerData);
    }

    @Override
    public void onBlockBreak(BlockBreak blockBreak) {
        if (blockBreak.action != DiggingAction.CANCELLED_DIGGING) {
            sentBreak = true;
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        }

        if (isTickPacket(event.getPacketType())) {
            if (sentBreak && !sentAnimation) {
                flagAndAlert();
            }

            sentAnimation = sentBreak = false;
        }
    }
}
