package br.edu.floodstats.domain.repository;

import br.edu.floodstats.domain.model.HydroRecord;

import java.util.List;

public interface HydrologyRepository {
    void saveAll(List<HydroRecord> records, String stationId) throws Exception;
}
