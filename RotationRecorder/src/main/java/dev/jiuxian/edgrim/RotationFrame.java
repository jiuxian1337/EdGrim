package dev.jiuxian.edgrim;

public final class RotationFrame {
    public final long sequence;
    public final float deltaYaw;
    public final float deltaPitch;

    public RotationFrame(long sequence, float deltaYaw, float deltaPitch) {
        this.sequence = sequence;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }
}
