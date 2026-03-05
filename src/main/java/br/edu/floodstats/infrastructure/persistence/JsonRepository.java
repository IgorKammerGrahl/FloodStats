package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.repository.HydrologyRepository;

import java.util.List;

public class JsonRepository implements HydrologyRepository {
    private final JsonPersistence persistence;

    public JsonRepository() {
        this.persistence = new JsonPersistence();
    }

    @Override
    public void saveAll(List<HydroRecord> records, String stationId) throws Exception {
        String safeStationId = stationId != null ? stationId.replaceAll(" ", "_").toLowerCase() : "unknown";
        java.nio.file.Path outputPath = java.nio.file.Paths.get("output/json");
        java.nio.file.Files.createDirectories(outputPath);
        String fileName = outputPath.resolve("dados_" + safeStationId + ".json").toString();
        persistence.save(records, fileName);
    }
}
