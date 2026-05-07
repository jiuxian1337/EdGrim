package tech.zkmjnic.edgrim.checks.impl.aim.analysis.a;

public final class ReferenceScore {
    public final String bestReferenceName;
    final double bestSimilarity;
    final double topAverage;

    ReferenceScore(String bestReferenceName, double bestSimilarity, double topAverage) {
        this.bestReferenceName = bestReferenceName;
        this.bestSimilarity = bestSimilarity;
        this.topAverage = topAverage;
    }

    static ReferenceScore empty() {
        return new ReferenceScore("", 0.0, 0.0);
    }

    public double combinedScore() {
        return (bestSimilarity * 0.7) + (topAverage * 0.3);
    }
}
