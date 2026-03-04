package br.edu.floodstats.presentation.cli;

import picocli.CommandLine.Command;

@Command(name = "floodstats", description = "FloodStats CLI - Sistema de Monitoramento de Enchentes", mixinStandardHelpOptions = true, version = "1.0", subcommands = {
        AnalyzeCommand.class })
public class FloodStatsCLI implements Runnable {

    @Override
    public void run() {
        // Se nenhum comando for passado, exibe a ajuda
        new picocli.CommandLine(this).usage(System.out);
    }
}
