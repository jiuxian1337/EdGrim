package tech.zkmjnic.edgrim.utils.anticheat.update;

import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tech.zkmjnic.edgrim.utils.data.SetBackData;
import tech.zkmjnic.edgrim.utils.data.TeleportData;

@AllArgsConstructor
@Getter
@Setter
public final class PositionUpdate {
    private final Vector3d from, to;
    private final boolean onGround;
    private final SetBackData setback;
    private final TeleportData teleportData;
    private boolean isTeleport;
}
