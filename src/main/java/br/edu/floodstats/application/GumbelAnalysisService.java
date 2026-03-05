package br.edu.floodstats.application;

import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.domain.gumbel.GumbelCalculator;
import br.edu.floodstats.domain.gumbel.GumbelResult;
import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.infrastructure.api.DataFetcher;
import br.edu.floodstats.infrastructure.factory.DataFetcherFactory;
import br.edu.floodstats.infrastructure.api.AnaHidroWebFetcher;
import br.edu.floodstats.domain.enums.PersistenceFormat;
import br.edu.floodstats.domain.repository.GumbelRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GumbelAnalysisService {

    private final List<DataFetcher> fetchers;
    private final List<GumbelRepository> gumbelRepositories;
    private final GumbelCalculator calculator;

    // DIP: Receiving the factory (could also be the list directly, either is an
    // abstraction)
    public GumbelAnalysisService(DataFetcherFactory fetcherFactory, List<GumbelRepository> gumbelRepositories) {
        this.fetchers = fetcherFactory.createAllFetchers();
        this.gumbelRepositories = gumbelRepositories;
        this.calculator = new GumbelCalculator();
    }

    public GumbelResult analyze(String stationCode, int startYear, int endYear) throws Exception {
        List<Double> annualMaxSeries = new ArrayList<>();

        // Find AnaHidroWebFetcher without explicit concrete instantiation
        DataFetcher anaFetcher = fetchers.stream()
                .filter(f -> f instanceof AnaHidroWebFetcher)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Serviço ANA não encontrado na injeção de dependência."));

        for (int year = startYear; year <= endYear; year++) {
            // UX: Progress logger with carriage return \r
            System.out.print(
                    "\r\u001B[36mExtraindo dados para análise de Gumbel: processando o ano " + year + "...\u001B[0m");

            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);

            AnalysisRequest request = new AnalysisRequest(
                    stationCode,
                    0.0, 0.0, // lat/lon (not used by ANA fetcher natively like OM)
                    startDate, endDate,
                    DataType.RIVER_DISCHARGE,
                    PersistenceFormat.NONE);

            List<HydroRecord> records = anaFetcher.fetch(request);

            // Extract the maximum flow for the year
            double maxFlow = records.stream()
                    .mapToDouble(HydroRecord::getValue)
                    .max()
                    .orElse(0.0);

            if (maxFlow > 0) {
                annualMaxSeries.add(maxFlow);
            }
        }
        System.out.println(
                "\n\u001B[32mExtração concluída. " + annualMaxSeries.size() + " anos capturados com sucesso.\u001B[0m");

        if (annualMaxSeries.isEmpty()) {
            throw new Exception("Nenhum dado válido encontrado para a série histórica especificada.");
        }

        List<Integer> returnPeriods = Arrays.asList(10, 50, 100);
        GumbelResult result = calculator.calculateReturnPeriods(annualMaxSeries, returnPeriods);

        result.setStationCode(stationCode);
        result.setStartYear(startYear);
        result.setEndYear(endYear);

        for (GumbelRepository repo : gumbelRepositories) {
            repo.save(result);
        }

        return result;
    }
}
