package br.edu.floodstats.application;

import br.edu.floodstats.domain.enums.PersistenceFormat;
import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;
import br.edu.floodstats.domain.stats.StatisticsCalculator;
import br.edu.floodstats.domain.stats.TrendCalculator;
import br.edu.floodstats.infrastructure.api.DataFetcher;
import br.edu.floodstats.infrastructure.factory.DataFetcherFactory;
import br.edu.floodstats.infrastructure.factory.PersistenceFactory;
import br.edu.floodstats.infrastructure.persistence.DataPersistence;
import br.edu.floodstats.infrastructure.report.HtmlReportGenerator;
import br.edu.floodstats.infrastructure.report.ReportGenerator;

import java.util.List;
import java.util.Scanner;

public class FloodAnalysisService {
    private final DataFetcherFactory fetcherFactory;
    private final PersistenceFactory persistenceFactory;
    private final StatisticsCalculator statsCalc;
    private final TrendCalculator trendCalc;
    private final ReportService reportService;
    private final ReportGenerator reportGenerator;

    public FloodAnalysisService() {
        this.fetcherFactory = new DataFetcherFactory();
        this.persistenceFactory = new PersistenceFactory();
        this.statsCalc = new StatisticsCalculator();
        this.trendCalc = new TrendCalculator();
        this.reportService = new ReportService();
        this.reportGenerator = new HtmlReportGenerator();
    }

    public void analyze(AnalysisRequest request, Scanner scanner) {
        String locName = request.getStationCode() != null ? request.getStationCode()
                : (request.getLatitude() + "_" + request.getLongitude());
        System.out.println("\nIniciando análise para " + locName + "...");

        try {
            // 1. Fetch Data
            System.out.println("Buscando dados das APIs públicas...");
            List<DataFetcher> fetchers = fetcherFactory.createAllFetchers();

            java.util.List<HydroRecord> anaRecords = new java.util.ArrayList<>();
            java.util.List<HydroRecord> openMeteoRecords = new java.util.ArrayList<>();

            for (DataFetcher fetcher : fetchers) {
                try {
                    // Evitar chamar fetcher de vazao se pedimos chuva, etc
                    // O DataFetcherFactory já retornava tudo, mas precisamos filtrar quais usar
                    if (request.getDataType() == br.edu.floodstats.domain.enums.DataType.RAINFALL
                            && fetcher instanceof br.edu.floodstats.infrastructure.api.OpenMeteoFloodFetcher)
                        continue;
                    if (request.getDataType() == br.edu.floodstats.domain.enums.DataType.RIVER_DISCHARGE
                            && fetcher instanceof br.edu.floodstats.infrastructure.api.OpenMeteoWeatherFetcher)
                        continue;

                    List<HydroRecord> fetchedRecords = fetcher.fetch(request);
                    if (fetcher instanceof br.edu.floodstats.infrastructure.api.AnaHidroWebFetcher) {
                        anaRecords.addAll(fetchedRecords);
                    } else {
                        openMeteoRecords.addAll(fetchedRecords);
                    }
                } catch (Exception e) {
                    System.out.println("Aviso: Falha ao buscar dados de " + fetcher.getClass().getSimpleName() + ": "
                            + e.getMessage());
                }
            }

            // Ground truth validation for River Discharge
            if (request.getDataType() == br.edu.floodstats.domain.enums.DataType.RIVER_DISCHARGE
                    && !anaRecords.isEmpty() && !openMeteoRecords.isEmpty()) {
                double anaAvg = anaRecords.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);
                double meteoAvg = openMeteoRecords.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);

                // Se a ANA diz que o rio é gigante e o OpenMeteo diz que é um riacho
                if (anaAvg > 100 && meteoAvg < 50) {
                    System.out.println("\n[ALERTA] Discrepância massiva detectada entre Satélite ("
                            + String.format("%.2f", meteoAvg) + ") e Estação Física ANA ("
                            + String.format("%.2f", anaAvg) + ").");
                    System.out.println(
                            "Descartando dados do satélite e utilizando a Estação ANA como Ground Truth absoluto.");
                    openMeteoRecords.clear(); // Discard OpenMeteo
                }
            }

            List<HydroRecord> records = new java.util.ArrayList<>();
            records.addAll(anaRecords);
            records.addAll(openMeteoRecords);

            if (records.isEmpty()) {
                System.out.println("Nenhum dado encontrado para os parâmetros informados.");
                return;
            }
            System.out.println(records.size() + " registros consolidados com sucesso.");

            // 2. Calculate Statistics
            System.out.println("Calculando estatísticas e tendência...");
            StatisticalResult partialResult = statsCalc.calculate(records);
            StatisticalResult finalResult = trendCalc.calculate(records, partialResult);

            // 3. Optional Persistence
            if (request.getPersistenceFormat() != PersistenceFormat.NONE) {
                System.out.println("Salvando dados localmente (" + request.getPersistenceFormat() + ")...");
                DataPersistence persistence = persistenceFactory.create(request.getPersistenceFormat());

                String fileName;
                if (request.getPersistenceFormat() == PersistenceFormat.SQLITE) {
                    fileName = "sqlite_database";
                } else {
                    String ext = request.getPersistenceFormat().toString().toLowerCase();
                    fileName = "dados_" + locName.replaceAll(" ", "_").toLowerCase() + "." + ext;
                }

                persistence.save(records, fileName);
                System.out.println("Dados salvos com sucesso.");
            }

            // 4. Generate Report
            String reportFileName = "relatorio_" + locName.replaceAll(" ", "_").toLowerCase()
                    + ".html";
            if (reportService.checkAndConfirmOverwrite(reportFileName, scanner)) {
                System.out.println("Gerando relatório HTML...");
                reportGenerator.generate(finalResult, records, reportFileName);
                System.out.println("Relatório gerado com sucesso: " + reportFileName);
            } else {
                System.out.println("Geração de relatório cancelada.");
            }

            showResultsSummary(finalResult, records.get(0).getUnit());

        } catch (Exception e) {
            System.err.println("Erro durante a análise: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showResultsSummary(StatisticalResult result, String unit) {
        System.out.println("\n--- Resumo Estatístico ---");
        System.out.printf("Tamanho da Amostra: %d dias\n", result.getSampleSize());
        System.out.printf("Média: %.2f %s\n", result.getMean(), unit);
        System.out.printf("Desvio Padrão: %.2f\n", result.getStandardDeviation());
        System.out.printf("Tendência: %s\n", result.getTrendDescription());
        System.out.println("--------------------------\n");
    }
}
