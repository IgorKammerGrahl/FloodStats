package br.edu.floodstats.presentation.tui;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.util.List;

public class FloodDashboardUI {

    public void showDashboard(StatisticalResult stats, List<HydroRecord> records, String unit) {
        try {
            DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
            Screen screen = terminalFactory.createScreen();
            screen.startScreen();

            // Setup TUI core components
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));
            BasicWindow window = new BasicWindow();
            window.setHints(List.of(Window.Hint.FULL_SCREEN, Window.Hint.NO_DECORATIONS));

            Panel mainPanel = new Panel(new BorderLayout());

            // Header Section
            Panel headerPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            String asciiArt = "  ______ _                 _  _____ _        _       \n" +
                    " |  ____| |               | |/ ____| |      | |      \n" +
                    " | |__  | | ___   ___   __| | (___ | |_ __ _| |_ ___ \n" +
                    " |  __| | |/ _ \\ / _ \\ / _` |\\___ \\| __/ _` | __/ __|\n" +
                    " | |    | | (_) | (_) | (_| |____) | || (_| | |_\\__ \\\n" +
                    " |_|    |_|\\___/ \\___/ \\__,_|_____/ \\__\\__,_|\\__|___/\n";

            Label artLabel = new Label(asciiArt);
            artLabel.setForegroundColor(TextColor.ANSI.CYAN);
            headerPanel.addComponent(artLabel);
            headerPanel.addComponent(new EmptySpace(new TerminalSize(0, 1)));

            mainPanel.addComponent(headerPanel, BorderLayout.Location.TOP);

            // Center Section (Split Grid)
            Panel dataPanel = new Panel(new GridLayout(2).setHorizontalSpacing(5));

            // Left side (Stats)
            Panel statsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
            statsPanel.addComponent(new Label("--- RESUMO ESTATÍSTICO ---").setForegroundColor(TextColor.ANSI.YELLOW));
            statsPanel.addComponent(new Label("Tamanho: " + stats.getSampleSize() + " dias"));
            statsPanel.addComponent(new Label(String.format("Média: %.2f %s", stats.getMean(), unit)));
            statsPanel.addComponent(new Label(String.format("Desvio P.: %.2f", stats.getStandardDeviation())));
            statsPanel.addComponent(new Label("Tendência: " + stats.getTrendDescription()));

            dataPanel.addComponent(statsPanel.withBorder(Borders.singleLine("Resultados")));

            // Right side (Table)
            Table<String> table = new Table<String>("Data", "Valor", "Unidade");
            for (HydroRecord rec : records) {
                table.getTableModel().addRow(String.valueOf(rec.getDate()), String.format("%.2f", rec.getValue()),
                        rec.getUnit());
            }

            Panel tablePanel = new Panel(new LinearLayout(Direction.VERTICAL));
            tablePanel.addComponent(table);

            dataPanel.addComponent(tablePanel.withBorder(Borders.singleLine("Histórico Bruto")));

            mainPanel.addComponent(dataPanel, BorderLayout.Location.CENTER);

            // Footer Section
            Panel footerPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            footerPanel.addComponent(new Label("Pressione ESC ou clique em Fechar no terminal para sair.")
                    .setForegroundColor(TextColor.ANSI.WHITE));
            mainPanel.addComponent(footerPanel, BorderLayout.Location.BOTTOM);

            // Map ESC key logic directly on window handler
            window.setComponent(mainPanel);

            // Simple key listener loop closing window via ESCAPE
            window.addWindowListener(new WindowListenerAdapter() {
                @Override
                public void onInput(Window baseWindow, com.googlecode.lanterna.input.KeyStroke keyStroke,
                        java.util.concurrent.atomic.AtomicBoolean deliverEvent) {
                    if (keyStroke.getKeyType() == com.googlecode.lanterna.input.KeyType.Escape) {
                        baseWindow.close();
                    }
                }
            });

            gui.addWindowAndWait(window);

            screen.stopScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
