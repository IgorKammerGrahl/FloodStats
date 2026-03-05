package br.edu.floodstats.presentation.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import br.edu.floodstats.application.GumbelAnalysisService;
import br.edu.floodstats.domain.gumbel.GumbelResult;
import br.edu.floodstats.infrastructure.factory.DataFetcherFactory;
import br.edu.floodstats.infrastructure.report.HtmlReportGenerator;

import br.edu.floodstats.domain.repository.GumbelRepository;
import br.edu.floodstats.infrastructure.persistence.JsonGumbelRepository;
import br.edu.floodstats.infrastructure.persistence.SqliteGumbelRepository;
import br.edu.floodstats.infrastructure.persistence.XmlGumbelRepository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "analyze-gumbel", description = "Executa a previsão de enchentes utilizando a Distribuição de Gumbel", mixinStandardHelpOptions = true)
public class AnalyzeGumbelCommand implements Callable<Integer> {

    @Option(names = { "-l",
            "--location" }, description = "Código Numérico da Estação de 8 dígitos (Ex: 15400000)")
    private String locationName;

    @Option(names = { "--start-year" }, description = "Ano de Início (Ex: 2010)")
    private Integer startYear;

    @Option(names = { "--end-year" }, description = "Ano de Fim (Ex: 2020)")
    private Integer endYear;

    @Override
    public Integer call() {
        try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
            boolean isInteractive = (locationName == null || locationName.trim().isEmpty());

            if (isInteractive) {
                System.out.println(picocli.CommandLine.Help.Ansi.AUTO
                        .string("\n@|cyan Bem-vindo ao Assistente Interativo Gumbel!|@"));
                System.out.println("Deixe em branco e pressione [ENTER] para usar os valores padrão.\n");

                System.out.print("Digite o Código da Estação ANA [Padrão: 15400000]: ");
                String inLoc = scanner.nextLine().trim();
                locationName = inLoc.isEmpty() ? "15400000" : inLoc;

                if (startYear == null) {
                    System.out.print("Ano de Início [Padrão: 2010]: ");
                    String inStart = scanner.nextLine().trim();
                    startYear = inStart.isEmpty() ? 2010 : Integer.parseInt(inStart);
                }

                if (endYear == null) {
                    System.out.print("Ano de Fim [Padrão: 2020]: ");
                    String inEnd = scanner.nextLine().trim();
                    endYear = inEnd.isEmpty() ? 2020 : Integer.parseInt(inEnd);
                }
            } else {
                if (startYear == null)
                    startYear = 2010;
                if (endYear == null)
                    endYear = 2020;
            }

            System.out.println("Iniciando Análise de Gumbel (Valores Extremos Tipo I)...");
            DataFetcherFactory factory = new DataFetcherFactory();

            List<GumbelRepository> gumbelRepositories = Arrays.asList(
                    new JsonGumbelRepository(),
                    new XmlGumbelRepository(),
                    new SqliteGumbelRepository());

            GumbelAnalysisService service = new GumbelAnalysisService(factory, gumbelRepositories);

            GumbelResult result = service.analyze(locationName, startYear, endYear);

            // Print ASCII Table
            System.out.println("\n+------------------------------------------------+");
            System.out.println("|     PREVISÃO DE ENCHENTES (MÉTODO GUMBEL)      |");
            System.out.println("+------------------------------------------------+");
            System.out.println("| Período de Retorno (T) | Vazão Esperada (m³/s) |");
            System.out.println("+------------------------------------------------+");
            System.out.printf("| 10 Anos                | %10.2f            |\n", result.getExpectedFlow(10));
            System.out.printf("| 50 Anos                | %10.2f            |\n", result.getExpectedFlow(50));
            System.out.printf("| 100 Anos               | %10.2f            |\n", result.getExpectedFlow(100));
            System.out.println("+------------------------------------------------+");

            // Generate/Append Report
            String reportDirStr = "output/reports/";
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(reportDirStr));

            // Security fix: Sanitize input to prevent Path Traversal
            String safeLocationName = locationName.replaceAll("[^a-zA-Z0-9_\\.-]", "_");
            String reportFileName = reportDirStr + "relatorio_gumbel_" + safeLocationName + "_" + startYear + "_"
                    + endYear
                    + ".html";

            java.io.File reportFile = new java.io.File(reportFileName);
            if (reportFile.exists()) {
                System.out.print(picocli.CommandLine.Help.Ansi.AUTO
                        .string("@|yellow O arquivo '" + reportFileName
                                + "' já existe. Deseja sobrescrever? (S/N): |@"));

                String input = scanner.nextLine().trim().toUpperCase();
                if (!input.equals("S")) {
                    System.out.println(
                            picocli.CommandLine.Help.Ansi.AUTO.string("@|red Operação cancelada pelo usuário.|@"));
                    return 0;
                }
            }

            HtmlReportGenerator reportGenerator = new HtmlReportGenerator();
            reportGenerator.generateGumbelReport(result, locationName, startYear, endYear, reportFileName);

            System.out.println("\nRelatório HTML de Gumbel gerado em: " + reportFileName);
            System.out.println(picocli.CommandLine.Help.Ansi.AUTO
                    .string("\u001B[32mResultados de previsão persistidos com sucesso em SQLite, JSON e XML!\u001B[0m"));

            return 0;
        } catch (Exception e) {
            System.err.println("\nErro na execução de Gumbel: " + e.getMessage());
            // Security fix: Do not leak stack traces to the CLI user
            return 1;
        }
    }
}
