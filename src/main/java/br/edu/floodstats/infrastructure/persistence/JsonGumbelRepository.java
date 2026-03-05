package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.gumbel.GumbelResult;
import br.edu.floodstats.domain.repository.GumbelRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonGumbelRepository implements GumbelRepository {
    private final Gson gson;

    public JsonGumbelRepository() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void save(GumbelResult result) throws Exception {
        String safeStationId = result.getStationCode() != null
                ? result.getStationCode().replaceAll(" ", "_").toLowerCase()
                : "unknown";
        Path outputPath = Paths.get("output/json");
        Files.createDirectories(outputPath);
        String fileName = outputPath.resolve("gumbel_" + safeStationId + ".json").toString();

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(result, writer);
        }
    }
}
