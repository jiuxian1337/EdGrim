package tech.zkmjnic.edgrim.manager.init.stop;

import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;

public class TerminatePacketEvents implements StoppableInitable {
    @Override
    public void stop() {
        LogUtil.info("Terminating PacketEvents...");
        PacketEvents.getAPI().terminate();
    }
}
