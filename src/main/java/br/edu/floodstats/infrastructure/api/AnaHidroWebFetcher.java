package br.edu.floodstats.infrastructure.api;

import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;
import com.google.gson.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AnaHidroWebFetcher implements DataFetcher {

    private final AnaTokenService tokenService;

    private final HttpClient httpClient;

    public AnaHidroWebFetcher(AnaTokenService tokenService) {
        this.tokenService = tokenService;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public List<HydroRecord> fetch(AnalysisRequest request) throws DataFetchException {
        try {
            String token = tokenService.getToken();

            // Exemplo genérico de chamada. Request necessitaria ter a estação se fosse
            // real.
            // Para simplificar e bater com a documentação do projeto, passamos um cod base.
            String stationCode = "00000000";
            if (request.getStationCode() != null && !request.getStationCode().isEmpty()) {
                stationCode = java.net.URLEncoder.encode(request.getStationCode(),
                        java.nio.charset.StandardCharsets.UTF_8.toString());
            }

            String baseUrl = "https://www.ana.gov.br/hidrowebservice/EstacoesTelemetricas/HidroinfoanaSerieTelemetricaAdotada/v1";

            String codEstParam = java.net.URLEncoder.encode("Código da Estação",
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            String tipoFiltroParam = java.net.URLEncoder.encode("Tipo Filtro Data",
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            String dataBuscaParam = java.net.URLEncoder.encode("Data de Busca (yyyy-MM-dd)",
                    java.nio.charset.StandardCharsets.UTF_8.toString());
            String rangeParam = java.net.URLEncoder.encode("Range Intervalo de busca",
                    java.nio.charset.StandardCharsets.UTF_8.toString());

            LocalDate currentStartDate = request.getStartDate();
            LocalDate finalEndDate = request.getEndDate();
            if (finalEndDate == null) {
                finalEndDate = LocalDate.now();
            }

            List<HydroRecord> allRecords = new ArrayList<>();

            // UI Progress Bar Calculation
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(currentStartDate, finalEndDate);
            long totalBlocks = (totalDays / 30) + 1;
            long currentBlock = 0;

            while (!currentStartDate.isAfter(finalEndDate)) {
                currentBlock++;
                int percent = (int) (((double) currentBlock / totalBlocks) * 100);
                if (percent > 100)
                    percent = 100;

                int filledBars = percent / 10;
                StringBuilder barBuilder = new StringBuilder("[");
                for (int i = 0; i < 10; i++) {
                    if (i < filledBars)
                        barBuilder.append("█");
                    else
                        barBuilder.append("░");
                }
                barBuilder.append("]");

                System.out.print(
                        "\r\u001B[36m" + barBuilder.toString() + " " + percent + "% Baixando dados da ANA...\u001B[0m");

                String dataBuscaStr = currentStartDate.toString(); // Formato YYYY-MM-DD

                String url = String.format("%s?%s=%s&%s=DATA_LEITURA&%s=%s&%s=DIAS_30",
                        baseUrl, codEstParam, stationCode, tipoFiltroParam, dataBuscaParam, dataBuscaStr, rangeParam);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                int maxRetries = 3;
                int attempts = 0;
                boolean success = false;

                while (attempts < maxRetries && !success) {
                    HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        allRecords.addAll(
                                parseResponse(response.body(), request.getDataType(), request.getStationCode()));
                        success = true;
                    } else if (response.statusCode() >= 500 && response.statusCode() < 600) {
                        attempts++;
                        if (attempts < maxRetries) {
                            System.err.println("\n\u001B[33mInstabilidade na ANA (Erro " + response.statusCode()
                                    + "). Tentativa " + (attempts + 1) + " de " + maxRetries + "...\u001B[0m");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        } else {
                            System.err.println("\n\u001B[31mAviso API ANA: Falha ao obter dados após " + maxRetries
                                    + " tentativas para data " + dataBuscaStr + " (HTTP " + response.statusCode()
                                    + ")\u001B[0m");
                        }
                    } else {
                        System.err.println("\n\u001B[33mAviso API ANA: HTTP " + response.statusCode() + " para data "
                                + dataBuscaStr + "\u001B[0m");
                        break;
                    }
                }

                currentStartDate = currentStartDate.plusDays(30);

                if (!currentStartDate.isAfter(finalEndDate)) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            System.out.println(); // Quebra de linha ao finalizar loop

            // Deduplication and sorting, strict date boundary check
            java.util.Map<LocalDate, HydroRecord> uniqueRecords = new java.util.LinkedHashMap<>();
            for (HydroRecord record : allRecords) {
                if (!record.getDate().isBefore(request.getStartDate()) && !record.getDate().isAfter(finalEndDate)) {
                    uniqueRecords.put(record.getDate(), record);
                }
            }
            List<HydroRecord> sortedRecords = new ArrayList<>(uniqueRecords.values());
            sortedRecords.sort(java.util.Comparator.comparing(HydroRecord::getDate));

            return sortedRecords;

        } catch (Exception e) {
            throw new DataFetchException("Falha ao buscar dados (HTTP): " + e.getMessage());
        }
    }

    private List<HydroRecord> parseResponse(String jsonBody, br.edu.floodstats.domain.enums.DataType dataType,
            String locationName) {
        List<HydroRecord> records = new ArrayList<>();
        JsonObject responseObj = JsonParser.parseString(jsonBody).getAsJsonObject();
        JsonArray items = responseObj.getAsJsonArray("items");

        // Usando mapa para deduplicação diária
        java.util.Map<LocalDate, Double> dailyAverages = new java.util.HashMap<>();
        java.util.Map<LocalDate, Integer> dailyCounts = new java.util.HashMap<>();

        // DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd
        // HH:mm:ss"); // Não precisaremos mais assim

        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();

            String rawDateStr = null;
            if (item.has("DataHora") && !item.get("DataHora").isJsonNull()) {
                rawDateStr = item.get("DataHora").getAsString();
            } else if (item.has("Data_Hora_Medicao") && !item.get("Data_Hora_Medicao").isJsonNull()) {
                rawDateStr = item.get("Data_Hora_Medicao").getAsString();
            }

            if (rawDateStr == null || rawDateStr.length() < 10)
                continue;

            LocalDate date;
            try {
                // Extrai apenas os 10 primeiros caracteres: 'YYYY-MM-DD'
                date = LocalDate.parse(rawDateStr.substring(0, 10));
            } catch (Exception e) {
                continue; // Ignora se não der pra ler a data
            }

            Double value = null;
            // Tratamento de Chuva
            if (br.edu.floodstats.domain.enums.DataType.RAINFALL.equals(dataType)) {
                if (item.has("Chuva_Adotada") && !item.get("Chuva_Adotada").isJsonNull()) {
                    try {
                        value = Double.parseDouble(item.get("Chuva_Adotada").getAsString().replace(",", "."));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            // Tratamento de Vazao
            else if (br.edu.floodstats.domain.enums.DataType.RIVER_DISCHARGE.equals(dataType)) {
                if (item.has("Vazao_Adotada") && !item.get("Vazao_Adotada").isJsonNull()) {
                    try {
                        value = Double.parseDouble(item.get("Vazao_Adotada").getAsString().replace(",", "."));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (value != null && value > 0.0) {
                dailyAverages.put(date, dailyAverages.getOrDefault(date, 0.0) + value);
                dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + 1);
            }
        }

        // Finaliza instanciando um HydroRecord por dia
        String unit = br.edu.floodstats.domain.enums.DataType.RAINFALL.equals(dataType) ? "mm" : "m³/s";
        String finalLocation = (locationName != null && !locationName.isEmpty()) ? locationName : "Estação ANA";
        for (java.util.Map.Entry<LocalDate, Double> entry : dailyAverages.entrySet()) {
            LocalDate date = entry.getKey();
            double avgValue = entry.getValue() / dailyCounts.get(date);

            if (br.edu.floodstats.domain.enums.DataType.RAINFALL.equals(dataType)) {
                records.add(new RainfallRecord(date, avgValue, unit, finalLocation));
            } else {
                records.add(new RiverDischargeRecord(date, avgValue, unit, finalLocation));
            }
        }

        return records;
    }

}
