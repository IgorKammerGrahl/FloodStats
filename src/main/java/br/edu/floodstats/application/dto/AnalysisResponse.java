package br.edu.floodstats.application.dto;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;

import java.util.List;

public record AnalysisResponse(StatisticalResult stats, List<HydroRecord> records, String unit) {
}
