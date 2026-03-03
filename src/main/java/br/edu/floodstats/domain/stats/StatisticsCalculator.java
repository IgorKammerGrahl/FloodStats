package br.edu.floodstats.domain.stats;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsCalculator {

    public StatisticalResult calculate(List<HydroRecord> records) {
        if (records == null || records.isEmpty()) {
            return new StatisticalResult(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int size = records.size();
        List<Double> values = records.stream()
                .map(HydroRecord::getValue)
                .sorted()
                .collect(Collectors.toList());

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / size;

        double median;
        if (size % 2 == 0) {
            median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            median = values.get(size / 2);
        }

        double mode = calculateMode(values);

        double varianceSum = 0;
        for (double v : values) {
            varianceSum += Math.pow(v - mean, 2);
        }
        double variance = size > 1 ? varianceSum / (size - 1) : 0; // Amostral
        double standardDeviation = Math.sqrt(variance);

        return new StatisticalResult(mean, median, mode, variance, standardDeviation, 0, 0, size);
    }

    private double calculateMode(List<Double> values) {
        Map<Double, Integer> frequencyMap = new HashMap<>();
        for (Double value : values) {
            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
        }

        double mode = 0;
        int maxCount = 0;
        for (Map.Entry<Double, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mode = entry.getKey();
            }
        }
        return mode;
    }
}
