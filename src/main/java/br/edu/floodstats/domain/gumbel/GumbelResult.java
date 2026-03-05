package br.edu.floodstats.domain.gumbel;

import java.util.Map;
import java.util.LinkedHashMap;

public class GumbelResult {
    private String stationCode;
    private int startYear;
    private int endYear;
    private double flow10Years;
    private double flow50Years;
    private double flow100Years;

    private final double mean;
    private final double standardDeviation;
    private final Map<Integer, Double> returnPeriodFlows;

    public GumbelResult(double mean, double standardDeviation) {
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.returnPeriodFlows = new LinkedHashMap<>();
    }

    public void addReturnPeriodResult(int t, double expectedFlow) {
        returnPeriodFlows.put(t, expectedFlow);
        if (t == 10)
            this.flow10Years = expectedFlow;
        if (t == 50)
            this.flow50Years = expectedFlow;
        if (t == 100)
            this.flow100Years = expectedFlow;
    }

    public double getExpectedFlow(int t) {
        return returnPeriodFlows.getOrDefault(t, 0.0);
    }

    public Map<Integer, Double> getReturnPeriodFlows() {
        return returnPeriodFlows;
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public int getStartYear() {
        return startYear;
    }

    public void setStartYear(int startYear) {
        this.startYear = startYear;
    }

    public int getEndYear() {
        return endYear;
    }

    public void setEndYear(int endYear) {
        this.endYear = endYear;
    }

    public double getFlow10Years() {
        return flow10Years;
    }

    public double getFlow50Years() {
        return flow50Years;
    }

    public double getFlow100Years() {
        return flow100Years;
    }
}
