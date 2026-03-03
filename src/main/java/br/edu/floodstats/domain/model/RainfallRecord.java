package br.edu.floodstats.domain.model;

import java.time.LocalDate;

public class RainfallRecord extends HydroRecord {
    public RainfallRecord() { super(); } // JAXB

    public RainfallRecord(LocalDate date, double value, String unit, String locationName) {
        super(date, value, unit, locationName);
    }

    @Override
    public String getDataTypeName() {
        return "Precipitação (mm)";
    }
}
