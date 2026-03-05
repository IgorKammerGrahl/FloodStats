package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.repository.HydrologyRepository;

import java.util.List;

public class SqliteRepository implements HydrologyRepository {
    private final SqlitePersistence persistence;

    public SqliteRepository() {
        this.persistence = new SqlitePersistence();
    }

    @Override
    public void saveAll(List<HydroRecord> records, String stationId) throws Exception {
        persistence.save(records, "sqlite_database");
    }
}
