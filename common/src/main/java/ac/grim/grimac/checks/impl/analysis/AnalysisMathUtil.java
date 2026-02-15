package ac.grim.grimac.checks.impl.analysis;

import ac.grim.grimac.utils.lists.Tuple;
import ac.grim.grimac.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class AnalysisMathUtil {
    private AnalysisMathUtil() {
    }

    public static double exponentialWeightedMean(Collection<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double alpha = 2.0 / (values.size() + 1.0);
        double result = 0.0;
        boolean first = true;
        for (double value : values) {
            if (first) {
                result = value;
                first = false;
            } else {
                result = alpha * value + (1.0 - alpha) * result;
            }
        }
        return result;
    }

    public static double pearsonCorrelation(List<Float> x, List<Float> y) {
        int size = Math.min(x.size(), y.size());
        if (size == 0) {
            return 0.0;
        }
        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < size; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
        }
        double meanX = sumX / size;
        double meanY = sumY / size;
        double num = 0.0;
        double denX = 0.0;
        double denY = 0.0;
        for (int i = 0; i < size; i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }
        double den = Math.sqrt(denX * denY);
        return den == 0.0 ? 0.0 : num / den;
    }

    public static List<Double> zScoreOutliers(List<? extends Number> values, double threshold) {
        List<Double> list = new ArrayList<>(values.size());
        for (Number n : values) {
            list.add(n.doubleValue());
        }
        double mean = MathUtil.getAverage(list);
        double std = MathUtil.stdDev(list, mean);
        List<Double> outliers = new ArrayList<>();
        if (std == 0.0) {
            return outliers;
        }
        for (double v : list) {
            double z = Math.abs(v - mean) / std;
            if (z >= threshold) {
                outliers.add(v);
            }
        }
        return outliers;
    }

    public static double entropy(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = max - min;
        if (range == 0.0) {
            return 0.0;
        }
        int bins = Math.min(12, Math.max(4, values.size() / 4));
        int[] counts = new int[bins];
        double binSize = range / bins;
        for (double v : values) {
            int idx = (int) ((v - min) / binSize);
            if (idx >= bins) {
                idx = bins - 1;
            }
            counts[idx]++;
        }
        double entropy = 0.0;
        int total = values.size();
        for (int count : counts) {
            if (count > 0) {
                double p = (double) count / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }

    public static Tuple<List<Double>, List<Double>> analyzeOutliers(List<Integer> values) {
        List<Double> data = new ArrayList<>(values.size());
        for (int v : values) {
            data.add((double) v);
        }
        data.sort(Double::compareTo);
        if (data.isEmpty()) {
            return new Tuple<>(new ArrayList<>(), new ArrayList<>());
        }
        double q1 = MathUtil.getMedian(data.subList(0, data.size() / 2));
        double q3 = MathUtil.getMedian(data.subList(data.size() / 2, data.size()));
        double iqr = Math.abs(q3 - q1);
        double low = q1 - 1.5 * iqr;
        double high = q3 + 1.5 * iqr;
        List<Double> lowOut = new ArrayList<>();
        List<Double> highOut = new ArrayList<>();
        for (double v : data) {
            if (v < low) {
                lowOut.add(v);
            } else if (v > high) {
                highOut.add(v);
            }
        }
        return new Tuple<>(lowOut, highOut);
    }

    public static double shannonEntropy(List<Float> data) {
        return MathUtil.calculateEntropy(data, 12);
    }

    public static double microChangeEntropy(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        List<Double> diffs = new ArrayList<>(values.size() - 1);
        for (int i = 1; i < values.size(); i++) {
            diffs.add(Math.abs(values.get(i) - values.get(i - 1)));
        }
        return entropy(diffs);
    }

    public static double stabilityIndex(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double mean = MathUtil.getAverage(values);
        double std = MathUtil.stdDev(values, mean);
        if (mean == 0.0) {
            return 0.0;
        }
        double cv = std / Math.abs(mean);
        return 1.0 - Math.min(1.0, cv);
    }

    public static double kireikoGeneric(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double avg = MathUtil.getAverage(values);
        double std = MathUtil.stdDev(values, avg);
        return std * 2.5 + avg * 0.75;
    }

    public static double rollingStdDev(List<Integer> data, int window) {
        if (data.size() < window || window < 1) {
            return 0.0;
        }
        double sum = 0.0;
        double squareSum = 0.0;
        for (int i = 0; i < window; i++) {
            sum += data.get(i);
            squareSum += Math.pow(data.get(i), 2);
        }
        double maxStd = 0.0;
        for (int i = window; i <= data.size(); i++) {
            double variance = (squareSum - Math.pow(sum, 2) / window) / window;
            double std = Math.sqrt(variance);
            if (std > maxStd) {
                maxStd = std;
            }
            if (i < data.size()) {
                sum += data.get(i) - data.get(i - window);
                squareSum += Math.pow(data.get(i), 2) - Math.pow(data.get(i - window), 2);
            }
        }
        return maxStd;
    }
}
