package tech.zkmjnic.edgrim.manager.init.stop;

import com.github.retrooper.packetevents.PacketEvents;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;

public class TerminatePacketEvents implements StoppableInitable {
    @Override
    public void stop() {
        LogUtil.info("Terminating PacketEvents...");
        PacketEvents.getAPI().terminate();
    }
}
