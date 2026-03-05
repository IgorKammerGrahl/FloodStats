package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.repository.HydrologyRepository;

import java.util.List;

public class XmlRepository implements HydrologyRepository {
    private final XmlPersistence persistence;

    public XmlRepository() {
        this.persistence = new XmlPersistence();
    }

    @Override
    public void saveAll(List<HydroRecord> records, String stationId) throws Exception {
        String safeStationId = stationId != null ? stationId.replaceAll(" ", "_").toLowerCase() : "unknown";
        java.nio.file.Path outputPath = java.nio.file.Paths.get("output/xml");
        java.nio.file.Files.createDirectories(outputPath);
        String fileName = outputPath.resolve("dados_" + safeStationId + ".xml").toString();
        persistence.save(records, fileName);
    }
}
