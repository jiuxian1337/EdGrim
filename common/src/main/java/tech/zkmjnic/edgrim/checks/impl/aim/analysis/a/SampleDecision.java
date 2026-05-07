package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

public final class SampleDecision {
    public final Label label;
    public final double legitScore;
    public final double cheatScore;

    public SampleDecision(Label label, double legitScore, double cheatScore) {
        this.label = label;
        this.legitScore = legitScore;
        this.cheatScore = cheatScore;
    }
}
