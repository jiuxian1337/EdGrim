package tech.zkmjnic.edgrim.checks.impl.breaking;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.BlockBreakCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.anticheat.update.BlockBreak;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;

@CheckData(name = "NoSwingBreak", description = "Did not swing while breaking block", experimental = true)
public class NoSwingBreak extends Check implements BlockBreakCheck {
    private boolean sentAnimation;
    private boolean sentBreak;

    public NoSwingBreak(EdGrimPlayer playerData) {
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
