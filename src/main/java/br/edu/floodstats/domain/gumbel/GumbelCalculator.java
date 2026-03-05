package br.edu.floodstats.domain.gumbel;

import java.util.List;

public class GumbelCalculator {

    public GumbelResult calculateReturnPeriods(List<Double> annualMaxSeries, List<Integer> returnPeriods) {
        if (annualMaxSeries == null || annualMaxSeries.isEmpty()) {
            throw new IllegalArgumentException("A série de máximos anuais não pode ser vazia.");
        }

        double mean = calculateMean(annualMaxSeries);
        double stdDev = calculateStandardDeviation(annualMaxSeries, mean);

        GumbelResult result = new GumbelResult(mean, stdDev);

        for (int t : returnPeriods) {
            double expectedFlow = calculateExpectedFlow(t, mean, stdDev);
            result.addReturnPeriodResult(t, expectedFlow);
        }

        return result;
    }

    public double calculateExpectedFlow(int t, double mean, double stdDev) {
        // Reduced Variable: yT = -Math.log(Math.log((double) T / (T - 1)))
        double yT = -Math.log(Math.log((double) t / (t - 1)));

        // Frequency Factor K = (yT - 0.5772) / 1.2825
        double k = (yT - 0.5772) / 1.2825;

        // Final Extreme Expected Flow: XT = x_mean + (K * sigma)
        return mean + (k * stdDev);
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() <= 1)
            return 0.0;
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum() / (values.size() - 1); // Sample standard deviation
        return Math.sqrt(variance);
    }
}
