package br.edu.floodstats.infrastructure.report;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.StatisticalResult;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class HtmlReportGenerator implements ReportGenerator {

        @Override
        public void generate(StatisticalResult result, List<HydroRecord> records, String outputPath) throws Exception {
                if (records == null || records.isEmpty())
                        return;

                List<HydroRecord> sortedRecords = records.stream()
                                .sorted(Comparator.comparing(HydroRecord::getDate))
                                .toList();

                String location = records.get(0).getLocationName();
                String startDate = sortedRecords.get(0).getDate().toString();
                String endDate = sortedRecords.get(sortedRecords.size() - 1).getDate().toString();
                String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String dataType = records.get(0).getDataTypeName();
                String unit = records.get(0).getUnit();

                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n")
                                .append("<title>FloodStats — Relatório de Análise</title>\n")
                                .append("<style>\n")
                                .append("body { font-family: Arial, sans-serif; margin: 40px auto; max-width: 900px; line-height: 1.6; color: #333; }\n")
                                .append("h1, h2 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 5px; }\n")
                                .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n")
                                .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n")
                                .append("th { background-color: #f4f7f6; color: #2c3e50; font-weight: bold; }\n")
                                .append("tr:nth-child(even) { background-color: #fbfbfb; }\n")
                                .append("svg { display: block; margin: 30px auto; border: 1px solid #ccc; background-color: #fafafa; }\n")
                                .append("footer { text-align: center; margin-top: 50px; font-size: 0.9em; color: #777; border-top: 1px solid #eee; padding-top: 20px; }\n")
                                .append(".header-info { background: #f9f9f9; padding: 15px; border-radius: 8px; margin-bottom: 30px; }\n")
                                .append(".header-info p { margin: 5px 0; font-weight: 500; }\n")
                                .append("</style>\n</head>\n<body>\n");

                // Header
                html.append("<header>\n<h1>Relatório de Análise Hidrológica</h1>\n")
                                .append("<div class=\"header-info\">\n")
                                .append(String.format("<p><strong>Local:</strong> %s</p>%n", location))
                                .append(String.format("<p><strong>Período:</strong> %s a %s</p>%n", startDate, endDate))
                                .append(String.format("<p><strong>Tipo de Dado:</strong> %s</p>%n", dataType))
                                .append(String.format("<p><strong>Gerado em:</strong> %s</p>%n", generatedAt))
                                .append("</div>\n</header>\n");

                // Statistics section
                html.append("<section id=\"estatisticas\">\n<h2>Resultados Estatísticos</h2>\n<table>\n")
                                .append("<tr><th>Métrica</th><th>Valor</th></tr>\n")
                                .append(String.format("<tr><td>Média</td><td>%.2f %s</td></tr>%n", result.getMean(),
                                                unit))
                                .append(String.format("<tr><td>Mediana</td><td>%.2f %s</td></tr>%n", result.getMedian(),
                                                unit))
                                .append(String.format("<tr><td>Moda</td><td>%.2f %s</td></tr>%n", result.getMode(),
                                                unit))
                                .append(String.format("<tr><td>Variância</td><td>%.2f</td></tr>%n",
                                                result.getVariance()))
                                .append(String.format("<tr><td>Desvio Padrão</td><td>%.2f</td></tr>%n",
                                                result.getStandardDeviation()))
                                .append(String.format("<tr><td>Tendência (%s/dia)</td><td>%.4f ( %s )</td></tr>%n",
                                                unit,
                                                result.getTrendSlope(), result.getTrendDescription()))
                                .append("</table>\n</section>\n");

                // Chart section
                html.append("<section id=\"grafico\">\n<h2>Gráfico Temporal</h2>\n")
                                .append(buildSvgChart(sortedRecords, result.getTrendSlope(),
                                                result.getTrendIntercept()))
                                .append("</section>\n");

                // Data section
                html.append("<section id=\"dados\">\n<h2>Dados Coletados (Amostra de 15 registros)</h2>\n<table>\n")
                                .append("<tr><th>Data</th><th>Valor</th><th>Unidade</th></tr>\n");

                int sampleLimit = Math.min(sortedRecords.size(), 15);
                for (int i = 0; i < sampleLimit; i++) {
                        HydroRecord hydroRecord = sortedRecords.get(i);
                        html.append(String.format("<tr><td>%s</td><td>%.2f</td><td>%s</td></tr>%n",
                                        hydroRecord.getDate(),
                                        hydroRecord.getValue(), hydroRecord.getUnit()));
                }
                if (sortedRecords.size() > 15) {
                        html.append(String.format("<tr><td colspan='3'>... e mais %d registros.</td></tr>%n",
                                        (sortedRecords.size() - 15)));
                }
                html.append("</table>\n</section>\n");

                // Footer
                html.append("<footer>\n<p>FloodStats v1.0 — Sistema de Monitoramento Estatístico de Enchentes</p>\n</footer>\n")
                                .append("</body>\n</html>");

                try (FileWriter writer = new FileWriter(outputPath)) {
                        writer.write(html.toString());
                }
        }

        private String buildSvgChart(List<HydroRecord> records, double slope, double intercept) {
                int svgWidth = 800;
                int svgHeight = 400;
                int padding = 50;
                int graphWidth = svgWidth - 2 * padding;
                int graphHeight = svgHeight - 2 * padding;

                double maxVal = records.stream().mapToDouble(HydroRecord::getValue).max().orElse(100);
                double minVal = records.stream().mapToDouble(HydroRecord::getValue).min().orElse(0);
                // Add a 10% top margin for aesthetic
                maxVal = maxVal + (maxVal * 0.1);

                StringBuilder svg = new StringBuilder();
                svg.append(String.format("<svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">%n",
                                svgWidth,
                                svgHeight));

                // Background and border
                svg.append(String.format("<rect width=\"%d\" height=\"%d\" fill=\"white\" />%n", svgWidth, svgHeight));

                // Axes
                svg.append(
                                String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"black\" stroke-width=\"2\" />%n",
                                                padding, svgHeight - padding, svgWidth - padding, svgHeight - padding)); // X-axis
                svg.append(
                                String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"black\" stroke-width=\"2\" />%n",
                                                padding, padding, padding, svgHeight - padding)); // Y-axis

                // Gridlines and Y-axis labels
                int numGridLines = 5;
                for (int i = 0; i <= numGridLines; i++) {
                        int y = svgHeight - padding - (i * graphHeight / numGridLines);
                        double val = minVal + (maxVal - minVal) * i / numGridLines;
                        svg.append(String.format(
                                        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"lightgray\" stroke-width=\"1\" />%n",
                                        padding, y, svgWidth - padding, y));
                        svg.append(String.format(
                                        "<text x=\"%d\" y=\"%d\" font-size=\"12\" text-anchor=\"end\" fill=\"black\">%.1f</text>%n",
                                        padding - 10, y + 5, val));
                }

                // Data Line (polyLine)
                svg.append("<polyline fill=\"none\" stroke=\"#3498db\" stroke-width=\"2\" points=\"");
                int numberOfRecords = records.size();
                for (int i = 0; i < numberOfRecords; i++) {
                        double val = records.get(i).getValue();
                        int x = padding + (i * graphWidth / Math.max(1, numberOfRecords - 1));
                        int y = (int) (svgHeight - padding - ((val - minVal) / (maxVal - minVal) * graphHeight));
                        svg.append(String.format("%d,%d ", x, y));
                }
                svg.append("\" />\n");

                // Trend Line
                if (numberOfRecords > 1 && (maxVal - minVal) > 0) {
                        double startVal = intercept;
                        double endVal = slope * (numberOfRecords - 1) + intercept;

                        int startX = padding;
                        int startY = (int) (svgHeight - padding
                                        - ((startVal - minVal) / (maxVal - minVal) * graphHeight));

                        int endX = svgWidth - padding;
                        int endY = (int) (svgHeight - padding - ((endVal - minVal) / (maxVal - minVal) * graphHeight));

                        svg.append(String.format(
                                        "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#e74c3c\" stroke-width=\"2\" stroke-dasharray=\"5,5\" />%n",
                                        startX, startY, endX, endY));

                        // Legend
                        svg.append(String.format(
                                        "<text x=\"%d\" y=\"%d\" font-size=\"14\" fill=\"#e74c3c\">Tendência</text>%n",
                                        svgWidth - padding - 80, padding + 20));
                        svg.append(String.format(
                                        "<text x=\"%d\" y=\"%d\" font-size=\"14\" fill=\"#3498db\">Dados Reais</text>%n",
                                        svgWidth - padding - 80, padding + 40));
                }

                svg.append("</svg>\n");
                return svg.toString();
        }

        public void generateGumbelReport(br.edu.floodstats.domain.gumbel.GumbelResult result, String location,
                        int startYear, int endYear, String outputPath) throws Exception {
                String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"UTF-8\">\n")
                                .append("<title>FloodStats — Relatório de Gumbel</title>\n")
                                .append("<style>\n")
                                .append("body { font-family: Arial, sans-serif; margin: 40px auto; max-width: 900px; line-height: 1.6; color: #333; }\n")
                                .append("h1, h2 { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 5px; }\n")
                                .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }\n")
                                .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n")
                                .append("th { background-color: #f4f7f6; color: #2c3e50; font-weight: bold; }\n")
                                .append("tr:nth-child(even) { background-color: #fbfbfb; }\n")
                                .append("footer { text-align: center; margin-top: 50px; font-size: 0.9em; color: #777; border-top: 1px solid #eee; padding-top: 20px; }\n")
                                .append(".header-info { background: #f9f9f9; padding: 15px; border-radius: 8px; margin-bottom: 30px; }\n")
                                .append(".header-info p { margin: 5px 0; font-weight: 500; }\n")
                                .append("</style>\n</head>\n<body>\n");

                html.append("<header>\n<h1>Previsão de Enchentes (Método Gumbel)</h1>\n")
                                .append("<div class=\"header-info\">\n")
                                .append(String.format("<p><strong>Local (Código ANA):</strong> %s</p>%n", location))
                                .append(String.format("<p><strong>Período da Série Histórica:</strong> %d a %d</p>%n",
                                                startYear, endYear))
                                .append(String.format("<p><strong>Gerado em:</strong> %s</p>%n", generatedAt))
                                .append("</div>\n</header>\n");

                html.append("<section id=\"gumbel\">\n<h2>Risco de Enchentes (Extremos Tipo I)</h2>\n<table>\n")
                                .append("<tr><th>Período de Retorno (T)</th><th>Vazão Máxima Esperada (m³/s)</th></tr>\n")
                                .append(String.format("<tr><td>10 Anos</td><td>%.2f</td></tr>%n",
                                                result.getExpectedFlow(10)))
                                .append(String.format("<tr><td>50 Anos</td><td>%.2f</td></tr>%n",
                                                result.getExpectedFlow(50)))
                                .append(String.format("<tr><td>100 Anos</td><td>%.2f</td></tr>%n",
                                                result.getExpectedFlow(100)))
                                .append("</table>\n</section>\n");

                html.append("<footer>\n<p>FloodStats v1.0 — Módulo de Previsão de Extremos</p>\n</footer>\n")
                                .append("</body>\n</html>");

                try (FileWriter writer = new FileWriter(outputPath)) {
                        writer.write(html.toString());
                }
        }
}
