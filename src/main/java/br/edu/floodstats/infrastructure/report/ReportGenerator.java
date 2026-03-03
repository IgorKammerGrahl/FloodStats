package br.edu.floodstats.infrastructure.report;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;

import java.util.List;

public interface ReportGenerator {
    void generate(StatisticalResult result, List<HydroRecord> records, String outputPath) throws Exception;
}
