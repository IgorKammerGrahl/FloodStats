package br.edu.floodstats.domain.model;

public class StatisticalResult {
    private final double mean;
    private final double median;
    private final double mode;
    private final double variance;
    private final double standardDeviation;
    private final double trendSlope;
    private final double trendIntercept;
    private final int sampleSize;

    public StatisticalResult(double mean, double median, double mode, double variance,
            double standardDeviation, double trendSlope,
            double trendIntercept, int sampleSize) {
        this.mean = mean;
        this.median = median;
        this.mode = mode;
        this.variance = variance;
        this.standardDeviation = standardDeviation;
        this.trendSlope = trendSlope;
        this.trendIntercept = trendIntercept;
        this.sampleSize = sampleSize;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getMode() {
        return mode;
    }

    public double getVariance() {
        return variance;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public double getTrendSlope() {
        return trendSlope;
    }

    public double getTrendIntercept() {
        return trendIntercept;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public StatisticalResult withTrend(double slope, double intercept) {
        return new StatisticalResult(this.mean, this.median, this.mode, this.variance,
                this.standardDeviation, slope, intercept, this.sampleSize);
    }

    public String getTrendDescription() {
        if (trendSlope > 0)
            return "Tendência de Alta";
        if (trendSlope < 0)
            return "Tendência de Baixa";
        return "Estável";
    }
}
