package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

public final class RecordedRotation {
    final float deltaYaw;
    final float deltaPitch;

    public RecordedRotation(float deltaYaw, float deltaPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }
}
