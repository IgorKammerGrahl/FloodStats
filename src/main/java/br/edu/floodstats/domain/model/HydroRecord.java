package br.edu.floodstats.domain.model;

import java.time.LocalDate;

public abstract class HydroRecord {
    private LocalDate date;
    private double value;
    private String unit;
    private String locationName;

    public HydroRecord() {} // JAXB

    public HydroRecord(LocalDate date, double value, String unit, String locationName) {
        this.date = date;
        this.value = value;
        this.unit = unit;
        this.locationName = locationName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) { this.date = date; }

    public double getValue() {
        return value;
    }

    public void setValue(double value) { this.value = value; }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) { this.unit = unit; }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) { this.locationName = locationName; }

    public abstract String getDataTypeName();
}
