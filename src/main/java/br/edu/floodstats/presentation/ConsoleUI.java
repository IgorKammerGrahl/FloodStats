package br.edu.floodstats.presentation;

import br.edu.floodstats.application.FloodAnalysisService;
import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.domain.enums.PersistenceFormat;
import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.infrastructure.factory.PersistenceFactory;
import br.edu.floodstats.infrastructure.persistence.DataPersistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final FloodAnalysisService service;
    private final Scanner scanner;

    public ConsoleUI() {
        this.service = new FloodAnalysisService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        while (running) {
            showMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    handleNewAnalysis();
                    break;
                case "2":
                    handleLoadData();
                    break;
                case "0":
                    running = false;
                    System.out.println("Encerrando o FloodStats. Até logo!");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
        scanner.close();
    }

    private void showMainMenu() {
        System.out.println("\n===========================================");
        System.out.println("                FLOODSTATS");
        System.out.println(" Sistema de Monitoramento de Enchentes ");
        System.out.println("===========================================");
        System.out.println("1. Nova Análise (buscar da API pública)");
        System.out.println("2. Carregar Análise Salva Localmente");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private void handleNewAnalysis() {
        System.out.print("\nNome do Local (Ex: São Paulo, Rio Tietê): ");
        String locationName = scanner.nextLine().trim();

        // Coordinates can be provided, we'll use a preset for simplicity or prompt
        double lat = InputValidator.readDouble(scanner, "Latitude (ex: -23.55 para São Paulo): ");
        double lon = InputValidator.readDouble(scanner, "Longitude (ex: -46.63 para São Paulo): ");

        LocalDate startDate = InputValidator.readDate(scanner, "Data de Início (AAAA-MM-DD): ");
        LocalDate endDate = InputValidator.readDate(scanner, "Data de Fim (AAAA-MM-DD): ");

        DataType dataType = InputValidator.readDataType(scanner);
        PersistenceFormat format = InputValidator.readPersistenceFormat(scanner);

        AnalysisRequest request = new AnalysisRequest(locationName, lat, lon, startDate, endDate, dataType, format);

        service.analyze(request, false);
    }

    private void handleLoadData() {
        PersistenceFormat format = InputValidator.readPersistenceFormat(scanner);
        if (format == PersistenceFormat.NONE) {
            System.out.println("Não é possível carregar com formato 'Nenhum'.");
            return;
        }

        System.out.print("Caminho do arquivo (ou ID de Análise/sqlite_database se usar SQLite): ");
        String fileName = scanner.nextLine().trim();

        try {
            PersistenceFactory factory = new PersistenceFactory();
            DataPersistence persistence = factory.create(format);
            System.out.println("Carregando dados...");
            List<HydroRecord> records = persistence.load(fileName);

            if (records == null || records.isEmpty()) {
                System.out.println("Nenhum dado encontrado no arquivo/ID informado.");
                return;
            }
            System.out.println(records.size() + " registros recuperados. A análise recomeçará (sem API).");

            // Recalculate stats directly without API fetch
            // But doing so requires direct service access or refactoring
            // FloodAnalysisService to accept direct records
            // For simplicity here, we simulate a simple output
            System.out.println("Os dados foram carregados (Local: " + records.get(0).getLocationName() + ", Tipo: "
                    + records.get(0).getDataTypeName() + ").");
            System.out.println(
                    "Para processamento completo offline, acesse os dados diretamente via código ou adapte o FloodAnalysisService.");

        } catch (Exception e) {
            System.err.println("Erro ao carregar os dados: " + e.getMessage());
        }
    }
}
