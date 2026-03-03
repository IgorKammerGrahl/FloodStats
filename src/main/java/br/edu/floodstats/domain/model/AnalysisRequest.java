package br.edu.floodstats.domain.model;

import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.domain.enums.PersistenceFormat;
import java.time.LocalDate;

public class AnalysisRequest {
    private String stationCode;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private DataType dataType;
    private PersistenceFormat persistenceFormat;

    public AnalysisRequest(String stationCode, Double latitude, Double longitude,
            LocalDate startDate, LocalDate endDate, DataType dataType,
            PersistenceFormat persistenceFormat) {
        this.stationCode = stationCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dataType = dataType;
        this.persistenceFormat = persistenceFormat;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public PersistenceFormat getPersistenceFormat() {
        return persistenceFormat;
    }

    public void setPersistenceFormat(PersistenceFormat persistenceFormat) {
        this.persistenceFormat = persistenceFormat;
    }
}
