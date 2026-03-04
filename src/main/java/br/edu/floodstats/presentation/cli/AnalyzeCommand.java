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

    @Option(names = { "--lat" }, description = "Latitude")
    private Double latitude;

    @Option(names = { "--lon" }, description = "Longitude")
    private Double longitude;

    @Option(names = { "-s", "--start" }, description = "Data de Início (AAAA-MM-DD)")
    private String startDateStr;

    @Option(names = { "-e", "--end" }, description = "Data de Fim (AAAA-MM-DD)")
    private String endDateStr;

    @Option(names = { "-t",
            "--type" }, description = "Tipo de dado (RAINFALL, RIVER_DISCHARGE, WATER_LEVEL)")
    private DataType dataType;

    @Option(names = { "-p",
            "--persistence" }, description = "Formato de persistência (NONE, JSON, XML, CSV, SQLITE)", defaultValue = "NONE")
    private PersistenceFormat persistenceFormat;

    @Option(names = { "-f", "--force" }, description = "Sobrescreve o relatório caso já exista")
    private boolean force;

    @Override
    public Integer call() {
        try (Scanner scanner = new Scanner(System.in)) {

            if (latitude == null || longitude == null || startDateStr == null || endDateStr == null) {
                System.out.println(CommandLine.Help.Ansi.AUTO
                        .string("\n@|cyan Bem-vindo ao Assistente Interativo do FloodStats!|@"));
                System.out.println("Deixe em branco e pressione [ENTER] para usar os valores padrão.\n");

                System.out.print("Digite o Código da Estação ANA [Padrão: 15400000]: ");
                String inLoc = scanner.nextLine().trim();
                locationName = inLoc.isEmpty() ? "15400000" : inLoc;

                System.out.print("Digite a Latitude [Padrão: -8.76]: ");
                String inLat = scanner.nextLine().trim();
                latitude = inLat.isEmpty() ? -8.76 : Double.parseDouble(inLat.replace(",", "."));

                System.out.print("Digite a Longitude [Padrão: -63.90]: ");
                String inLon = scanner.nextLine().trim();
                longitude = inLon.isEmpty() ? -63.90 : Double.parseDouble(inLon.replace(",", "."));

                System.out.print("Data de Início (YYYY-MM-DD) [Padrão: 2020-01-01]: ");
                String inStart = scanner.nextLine().trim();
                startDateStr = inStart.isEmpty() ? "2020-01-01" : inStart;

                System.out.print("Data Final (YYYY-MM-DD) [Padrão: 2020-12-31]: ");
                String inEnd = scanner.nextLine().trim();
                endDateStr = inEnd.isEmpty() ? "2020-12-31" : inEnd;

                if (dataType == null)
                    dataType = DataType.RIVER_DISCHARGE;
            }

            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

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

                String input = scanner.nextLine().trim().toUpperCase();
                if (input.equals("S")) {
                    force = true;
                } else {
                    System.out.println(CommandLine.Help.Ansi.AUTO.string("@|red Operação cancelada pelo usuário.|@"));
                    return 0;
                }
            }

            AnalysisRequest request = new AnalysisRequest(locName, latitude, longitude, startDate, endDate, dataType,
                    persistenceFormat);
            FloodAnalysisService service = new FloodAnalysisService();

            service.analyze(request, force);

            return 0;
        } catch (Exception e) {
            System.err.println(CommandLine.Help.Ansi.AUTO.string("@|red,bold ERRO NA EXECUÇÃO:|@ " + e.getMessage()));
            return 1;
        }
    }
}
