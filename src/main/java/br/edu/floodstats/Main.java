package br.edu.floodstats;

import br.edu.floodstats.presentation.cli.FloodStatsCLI;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new FloodStatsCLI()).execute(args);
        System.exit(exitCode);
    }
}
