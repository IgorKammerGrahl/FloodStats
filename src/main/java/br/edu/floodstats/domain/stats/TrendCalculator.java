package br.edu.floodstats.domain.stats;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TrendCalculator {

    public StatisticalResult calculate(List<HydroRecord> records, StatisticalResult partialResult) {
        if (records == null || records.size() < 2) {
            return partialResult;
        }

        // Sort records by date to ensure chronological order
        List<HydroRecord> sortedRecords = records.stream()
                .sorted(Comparator.comparing(HydroRecord::getDate))
                .collect(Collectors.toList());

        int n = sortedRecords.size();
        long sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        long sumX2 = 0;

        for (int i = 0; i < n; i++) {
            long x = i;
            double y = sortedRecords.get(i).getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denom = (double) (n * sumX2 - sumX * sumX);
        if (denom == 0) {
            return partialResult;
        }

        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;

        return partialResult.withTrend(slope, intercept);
    }
}
