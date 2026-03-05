package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.gumbel.GumbelResult;
import br.edu.floodstats.domain.repository.GumbelRepository;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class XmlGumbelRepository implements GumbelRepository {

    @Override
    public void save(GumbelResult result) throws Exception {
        String safeStationId = result.getStationCode() != null
                ? result.getStationCode().replaceAll(" ", "_").toLowerCase()
                : "unknown";
        Path outputPath = Paths.get("output/xml");
        Files.createDirectories(outputPath);
        String fileName = outputPath.resolve("gumbel_" + safeStationId + ".xml").toString();

        StringBuilder xmlContent = new StringBuilder();
        xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlContent.append("<GumbelResult>\n");
        xmlContent.append("    <stationCode>").append(result.getStationCode()).append("</stationCode>\n");
        xmlContent.append("    <startYear>").append(result.getStartYear()).append("</startYear>\n");
        xmlContent.append("    <endYear>").append(result.getEndYear()).append("</endYear>\n");
        xmlContent.append("    <mean>").append(result.getMean()).append("</mean>\n");
        xmlContent.append("    <standardDeviation>").append(result.getStandardDeviation())
                .append("</standardDeviation>\n");
        xmlContent.append("    <flow10Years>").append(result.getFlow10Years()).append("</flow10Years>\n");
        xmlContent.append("    <flow50Years>").append(result.getFlow50Years()).append("</flow50Years>\n");
        xmlContent.append("    <flow100Years>").append(result.getFlow100Years()).append("</flow100Years>\n");
        xmlContent.append("</GumbelResult>");

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(xmlContent.toString());
        }
    }
}
