package br.edu.floodstats.domain.model;

import java.time.LocalDate;

public class RiverDischargeRecord extends HydroRecord {
    public RiverDischargeRecord() { super(); } // JAXB

    public RiverDischargeRecord(LocalDate date, double value, String unit, String locationName) {
        super(date, value, unit, locationName);
    }

    @Override
    public String getDataTypeName() {
        return "Vazão (m³/s)";
    }
}
