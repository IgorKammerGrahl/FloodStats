package br.edu.floodstats.presentation;

import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.domain.enums.PersistenceFormat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class InputValidator {

    private InputValidator() {
        // Private constructor to hide implicit one
    }

    @SuppressWarnings("squid:S106")
    public static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Digite um número válido (ex: -23.55).");
            }
        }
    }

    @SuppressWarnings("squid:S106")
    public static LocalDate readDate(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use o formato AAAA-MM-DD (ex: 2024-01-01).");
            }
        }
    }

    @SuppressWarnings("squid:S106")
    public static DataType readDataType(Scanner scanner) {
        while (true) {
            System.out.println("\nTipo de Dado:");
            System.out.println("1. Precipitação (Chuva)");
            System.out.println("2. Vazão de Rio");
            System.out.print("Escolha (1-2): ");
            String input = scanner.nextLine().trim();
            if (input.equals("1"))
                return DataType.RAINFALL;
            if (input.equals("2"))
                return DataType.RIVER_DISCHARGE;
            System.out.println("Opção inválida.");
        }
    }

    @SuppressWarnings("squid:S106")
    public static PersistenceFormat readPersistenceFormat(Scanner scanner) {
        while (true) {
            System.out.println("\nFormato de Persistência:");
            System.out.println("1. JSON");
            System.out.println("2. XML");
            System.out.println("3. SQLite");
            System.out.println("4. Nenhum (Apenas gerar HTML)");
            System.out.print("Escolha (1-4): ");
            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    return PersistenceFormat.JSON;
                case "2":
                    return PersistenceFormat.XML;
                case "3":
                    return PersistenceFormat.SQLITE;
                case "4":
                    return PersistenceFormat.NONE;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }
}
