package br.edu.floodstats.infrastructure.api;

import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;

import java.util.List;

public interface DataFetcher {
    List<HydroRecord> fetch(AnalysisRequest request) throws DataFetchException;
}
