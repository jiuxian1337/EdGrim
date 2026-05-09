package tech.zkmjnic.edgrim.checks.impl.analysis.a;

public final class RotationFrame {
    public final long sequence;
    public double deltaYaw;
    public double deltaPitch;

    public RotationFrame(long sequence, float deltaYaw, float deltaPitch) {
        this.sequence = sequence;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }
}
