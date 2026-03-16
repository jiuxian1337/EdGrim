package tech.zkmjnic.edgrim.checks.impl.crash;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;

@CheckData(name = "CrashH")
public class CrashH extends Check implements PacketCheck {

    public CrashH(EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);
            String text = wrapper.getText();
            final int length = text.length();
            // general length limit
            if (length > (!player.canUseGameMasterBlocks() ? 256 : 32500)) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                flagAndAlert("(length) length=" + length);
                return;
            }
            // paper's patch
            final int index;
            if (length > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
                if (shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
                flagAndAlert("(invalid) length=" + length);
            }
        }
    }
}
