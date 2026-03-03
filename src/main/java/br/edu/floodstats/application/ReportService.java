package br.edu.floodstats.application;

import java.io.File;
import java.util.Scanner;

public class ReportService {

    @SuppressWarnings("squid:S106")
    public boolean checkAndConfirmOverwrite(String outputPath, Scanner scanner) {
        File file = new File(outputPath);
        if (file.exists()) {
            System.out.printf("O arquivo '%s' já existe. Deseja sobrescrever? (S/N): ", outputPath);
            String input = scanner.nextLine().trim().toUpperCase();
            return input.equals("S");
        }
        return true;
    }
}
