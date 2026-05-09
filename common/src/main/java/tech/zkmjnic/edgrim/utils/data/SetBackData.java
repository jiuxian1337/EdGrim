package tech.zkmjnic.edgrim.utils.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.zkmjnic.edgrim.utils.math.Vector3dm;

@Getter
@Setter
@ToString
public class SetBackData {
    TeleportData teleportData;
    float xRot, yRot;
    Vector3dm velocity;
    boolean vehicle;
    boolean isComplete = false;
    // TODO: Rethink when we block movements for teleports, perhaps after 10 ticks or 5 blocks?
    boolean isPlugin;
    int ticksComplete = 0;

    public SetBackData(TeleportData teleportData, float xRot, float yRot, Vector3dm velocity, boolean vehicle, boolean isPlugin) {
        this.teleportData = teleportData;
        this.xRot = xRot;
        this.yRot = yRot;
        this.velocity = velocity;
        this.vehicle = vehicle;
        this.isPlugin = isPlugin;
    }

    public void tick() {
        if (isComplete) ticksComplete++;
    }
}
