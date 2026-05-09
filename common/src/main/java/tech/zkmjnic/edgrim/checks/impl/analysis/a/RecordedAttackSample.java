package tech.zkmjnic.edgrim.checks.impl.analysis.a;

import java.util.List;

public class RecordedAttackSample {
    public List<RecordedRotation> rotations;

    public RecordedAttackSample() {}

    public RecordedAttackSample(List<RecordedRotation> rotations) {
        this.rotations = rotations;
    }

    public List<RecordedRotation> rotations() {
        return rotations;
    }
}
