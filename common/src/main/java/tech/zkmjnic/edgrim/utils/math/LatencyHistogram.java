package tech.zkmjnic.edgrim.utils.math;

public final class LatencyHistogram {
    private static final int MAX_LATENCY = 1000;
    private static final int BUCKETS = 100;
    private static final int SCALE = MAX_LATENCY / BUCKETS;

    private final long[] occurrences = new long[BUCKETS + 1];
    private long size;

    public void addLatency(long latency) {
        if (latency > MAX_LATENCY || latency < 0) return;
        occurrences[(int) (Math.min(latency, MAX_LATENCY - 1) / SCALE)]++;
        size++;
        if (size > 9999) {
            for (int i = 0; i < BUCKETS; i++) occurrences[i] >>= 1;
            size >>= 1;
        }
    }

    public double mean() {
        long sum = 0;
        for (int i = 0; i < BUCKETS; i++) sum += occurrences[i] * i * SCALE;
        return (double) sum / size;
    }

    public double biasedStdDev(double requiredDistance) {
        double m = mean();
        double sum = 0;
        for (int i = 0; i < BUCKETS; i++) {
            double dist = Math.abs(i * SCALE - m);
            double weight = Math.exp(-dist / requiredDistance);
            sum += Math.pow(dist, 2) * occurrences[i] * weight;
        }
        return Math.max(Math.sqrt(sum / size), 25);
    }

    public double biasedProbabilityOf(long latency, double biasDistance) {
        double m = mean();
        if (latency < m) return 100;
        double stdDev = biasedStdDev(biasDistance);
        if (latency < m + stdDev) return 100;
        return Math.exp(-Math.pow(latency - m, 2) / (2 * Math.pow(stdDev, 2))) / (stdDev * Math.sqrt(2 * Math.PI));
    }

    public long getSize() { return size; }
}
