package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

import java.util.List;

public final class RecordedAttackSample {
    public final List<RecordedRotation> rotations;

    public RecordedAttackSample(List<RecordedRotation> rotations) {
        this.rotations = rotations;
    }
}
