package br.edu.floodstats.presentation.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import br.edu.floodstats.application.FloodAnalysisService;
import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.domain.enums.PersistenceFormat;
import br.edu.floodstats.domain.model.AnalysisRequest;

import java.io.File;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Command(name = "analyze", description = "Executa uma nova análise buscando dados de APIs públicas.", mixinStandardHelpOptions = true)
public class AnalyzeCommand implements Callable<Integer> {

    @Option(names = { "-l",
            "--location" }, description = "Código Numérico da Estação de 8 dígitos (Ex: 15400000)", defaultValue = "")
    private String locationName;

    @Option(names = { "--lat" }, description = "Latitude", required = true)
    private Double latitude;

    @Option(names = { "--lon" }, description = "Longitude", required = true)
    private Double longitude;

    @Option(names = { "-s", "--start" }, description = "Data de Início (AAAA-MM-DD)", required = true)
    private LocalDate startDate;

    @Option(names = { "-e", "--end" }, description = "Data de Fim (AAAA-MM-DD)", required = true)
    private LocalDate endDate;

    @Option(names = { "-t",
            "--type" }, description = "Tipo de dado (RAINFALL, RIVER_DISCHARGE, WATER_LEVEL)", required = true)
    private DataType dataType;

    @Option(names = { "-p",
            "--persistence" }, description = "Formato de persistência (NONE, JSON, XML, CSV, SQLITE)", defaultValue = "NONE")
    private PersistenceFormat persistenceFormat;

    @Option(names = { "-f", "--force" }, description = "Sobrescreve o relatório caso já exista")
    private boolean force;

    @Override
    public Integer call() {
        try {
            String locName = locationName;
            if (locName == null || locName.trim().isEmpty()) {
                locName = latitude + "_" + longitude;
            }

            String reportFileName = "relatorio_" + locName.replaceAll(" ", "_").toLowerCase() + ".html";
            File reportFile = new File(reportFileName);

            if (reportFile.exists() && !force) {
                System.out.print(CommandLine.Help.Ansi.AUTO
                        .string("@|yellow O arquivo '" + reportFileName
                                + "' já existe. Deseja sobrescrever? (S/N): |@"));
                try (Scanner scanner = new Scanner(System.in)) {
                    String input = scanner.nextLine().trim().toUpperCase();
                    if (input.equals("S")) {
                        force = true;
                    } else {
                        System.out
                                .println(CommandLine.Help.Ansi.AUTO.string("@|red Operação cancelada pelo usuário.|@"));
                        return 0;
                    }
                }
            }

            AnalysisRequest request = new AnalysisRequest(locName, latitude, longitude, startDate, endDate, dataType,
                    persistenceFormat);
            FloodAnalysisService service = new FloodAnalysisService();

            service.analyze(request, force);
            System.out.println(CommandLine.Help.Ansi.AUTO.string("@|green Análise concluída com sucesso!|@"));

            return 0;
        } catch (Exception e) {
            System.err.println(CommandLine.Help.Ansi.AUTO.string("@|red,bold ERRO NA EXECUÇÃO:|@ " + e.getMessage()));
            return 1;
        }
    }
}
