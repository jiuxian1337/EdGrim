package tech.zkmjnic.edgrim.checks.impl.aim.trajectory;

import tech.zkmjnic.edgrim.utils.math.MathUtil;

import java.util.List;

public interface StatisticalDetectionStrategy extends AimDetectionStrategy {
    default double calculateMean(List<Double> data) {
        return MathUtil.mean(data);
    }

    default double calculateStdDev(List<Double> data) {
        return MathUtil.getStandardDeviation(data);
    }

    default double calculateKurtosis(List<Double> data) {
        if (data.size() < 4) return 0;
        double mean = calculateMean(data);
        double stdDev = calculateStdDev(data);
        if (stdDev < 0.001) return 0;
        double sum = 0;
        for (double x : data) {
            sum += Math.pow((x - mean) / stdDev, 4);
        }
        return sum / data.size() - 3;
    }

    default double calculateSkewness(List<Double> data) {
        if (data.size() < 3) return 0;
        double mean = calculateMean(data);
        double stdDev = calculateStdDev(data);
        if (stdDev < 0.001) return 0;
        double sum = 0;
        for (double x : data) {
            sum += Math.pow((x - mean) / stdDev, 3);
        }
        return sum / data.size();
    }

    default double calculatePearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.size() < 2) return 0;
        double meanX = calculateMean(x);
        double meanY = calculateMean(y);
        double cov = 0;
        double varX = 0;
        double varY = 0;
        for (int i = 0; i < x.size(); i++) {
            double dx = x.get(i) - meanX;
            double dy = y.get(i) - meanY;
            cov += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        return (varX == 0 || varY == 0) ? 0 : cov / (Math.sqrt(varX) * Math.sqrt(varY));
    }
}
