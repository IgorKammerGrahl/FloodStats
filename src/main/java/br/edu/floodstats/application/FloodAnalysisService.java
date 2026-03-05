package br.edu.floodstats.application;

import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;
import br.edu.floodstats.domain.stats.StatisticsCalculator;
import br.edu.floodstats.domain.stats.TrendCalculator;
import br.edu.floodstats.infrastructure.api.DataFetcher;
import br.edu.floodstats.infrastructure.factory.DataFetcherFactory;
import br.edu.floodstats.infrastructure.report.HtmlReportGenerator;
import br.edu.floodstats.infrastructure.report.ReportGenerator;

import br.edu.floodstats.domain.repository.HydrologyRepository;
import br.edu.floodstats.infrastructure.persistence.JsonRepository;
import br.edu.floodstats.infrastructure.persistence.SqliteRepository;
import br.edu.floodstats.infrastructure.persistence.XmlRepository;

import java.util.ArrayList;
import java.util.List;

public class FloodAnalysisService {
    private final DataFetcherFactory fetcherFactory;
    private final StatisticsCalculator statsCalc;
    private final TrendCalculator trendCalc;
    private final ReportService reportService;
    private final ReportGenerator reportGenerator;
    private final List<HydrologyRepository> repositories;

    public FloodAnalysisService() {
        this.fetcherFactory = new DataFetcherFactory();
        this.statsCalc = new StatisticsCalculator();
        this.trendCalc = new TrendCalculator();
        this.reportService = new ReportService();
        this.reportGenerator = new HtmlReportGenerator();
        this.repositories = new ArrayList<>();
        this.repositories.add(new SqliteRepository());
        this.repositories.add(new JsonRepository());
        this.repositories.add(new XmlRepository());
    }

    public br.edu.floodstats.application.dto.AnalysisResponse analyze(AnalysisRequest request, boolean forceOverwrite) {
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
                    System.err.println(
                            picocli.CommandLine.Help.Ansi.AUTO.string("@|yellow Aviso: Falha ao buscar dados de "
                                    + fetcher.getClass().getSimpleName() + " -> " + e.getMessage() + "|@"));
                }
            }

            // Ground truth validation for River Discharge
            if (request.getDataType() == br.edu.floodstats.domain.enums.DataType.RIVER_DISCHARGE
                    && !anaRecords.isEmpty() && !openMeteoRecords.isEmpty()) {
                double anaAvg = anaRecords.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);
                double meteoAvg = openMeteoRecords.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);

                if (anaAvg > 100 && meteoAvg < 50) {
                    System.out.println("\n\u001B[31m[ALERTA] Discrepância massiva detectada entre Satélite (" // Red
                            + String.format("%.2f", meteoAvg) + ") e Estação Física ANA ("
                            + String.format("%.2f", anaAvg) + ").\u001B[0m");
                    System.out.println(
                            "\u001B[33mDescartando dados do satélite e utilizando a Estação ANA como Ground Truth absoluto.\u001B[0m"); // Yellow
                    openMeteoRecords.clear(); // Discard OpenMeteo
                }
            }

            List<HydroRecord> records = new java.util.ArrayList<>();
            records.addAll(anaRecords);
            records.addAll(openMeteoRecords);

            if (records.isEmpty()) {
                System.out.println("\u001B[31mNenhum dado encontrado para os parâmetros informados.\u001B[0m");
                return null;
            }
            System.out.println("\u001B[32m" + records.size() + " registros consolidados com sucesso.\u001B[0m"); // Green

            // 2. Calculate Statistics
            System.out.println("Calculando estatísticas e tendência...");
            StatisticalResult partialResult = statsCalc.calculate(records);
            StatisticalResult finalResult = trendCalc.calculate(records, partialResult);

            // 3. Optional Persistence
            System.out.println("Salvando dados simultaneamente em Banco de Dados, JSON e XML...");
            for (HydrologyRepository repository : repositories) {
                try {
                    repository.saveAll(records, locName);
                } catch (Exception persistenceEx) {
                    System.err.println(
                            picocli.CommandLine.Help.Ansi.AUTO.string("@|yellow Aviso: Falha ao salvar no repositório "
                                    + repository.getClass().getSimpleName() + " -> " + persistenceEx.getMessage()
                                    + "|@"));
                }
            }

            // 4. Generate Report
            String reportDirStr = "output/reports/";
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(reportDirStr));
            String reportFileName = reportDirStr + "relatorio_" + locName.replaceAll(" ", "_").toLowerCase()
                    + ".html";
            if (reportService.checkAndConfirmOverwrite(reportFileName, forceOverwrite)) {
                System.out.println("Gerando relatório HTML...");
                reportGenerator.generate(finalResult, records, reportFileName);
                System.out.println("Relatório gerado com sucesso: " + reportFileName);
            } else {
                System.out.println("Geração de relatório cancelada.");
            }

            showResultsSummary(finalResult, records.get(0).getUnit());

            return new br.edu.floodstats.application.dto.AnalysisResponse(finalResult, records,
                    records.get(0).getUnit());

        } catch (

        Exception e) {
            throw new RuntimeException("Falha de comunicação ou processamento: " + e.getMessage(), e);
        }
    }

    private void showResultsSummary(StatisticalResult result, String unit) {
        String reset = "\u001B[0m";
        String cyan = "\u001B[36m";
        String green = "\u001B[32m";

        System.out.println("\n" + cyan + "+------------------------------------------+");
        System.out.println("|" + reset + "            RESUMO ESTATÍSTICO            " + cyan + "|");
        System.out.println("+------------------------------------------+" + reset);

        System.out.printf(cyan + "|" + reset + " Tamanho da Amostra: %-21.21s" + cyan + "|\n",
                result.getSampleSize() + " dias");
        System.out.printf(cyan + "|" + reset + " Média:              %-21.21s" + cyan + "|\n",
                String.format("%.2f %s", result.getMean(), unit));
        System.out.printf(cyan + "|" + reset + " Desvio Padrão:      %-21.21s" + cyan + "|\n",
                String.format("%.2f", result.getStandardDeviation()));
        System.out.printf(cyan + "|" + reset + " Tendência:          %-21.21s" + cyan + "|\n",
                result.getTrendDescription());

        System.out.println(cyan + "+------------------------------------------+" + reset + "\n");

        System.out.println(green + "Análise concluída com sucesso!" + reset);
    }
}
