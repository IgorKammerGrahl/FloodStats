package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;

import java.util.List;

public interface DataPersistence {
    void save(List<HydroRecord> records, String fileName) throws Exception;

    List<HydroRecord> load(String fileName) throws Exception;
}
