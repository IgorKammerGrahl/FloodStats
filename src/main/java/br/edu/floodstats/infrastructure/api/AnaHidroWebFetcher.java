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

import br.edu.floodstats.infrastructure.config.ApiConfig;

public class AnaHidroWebFetcher implements DataFetcher {

    private final AnaTokenService tokenService;
    private final ApiConfig apiConfig;
    private final HttpClient httpClient;

    public AnaHidroWebFetcher(AnaTokenService tokenService) {
        this.tokenService = tokenService;
        this.apiConfig = new ApiConfig();
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
                stationCode = request.getStationCode(); // Ajuste simplificado
            }

            String url = String.format(
                    "%s/EstacoesTelemetricas/HidroinfoanaSerieTelemetricaAdotada/v1?CodigoDaEstacao=%s&TipoFiltroData=DATA_LEITURA&DataDeBusca=%s&RangeIntervaloDeBusca=DIAS_30",
                    apiConfig.getAnaApiUrl(),
                    stationCode,
                    request.getStartDate().toString());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), request.getDataType());
            } else {
                throw new DataFetchException("Erro API ANA: HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Erro na ANA: " + e.getMessage());
            e.printStackTrace();
            throw new DataFetchException("Falha ao buscar dados da ANA no HidroWeb: " + e.getMessage());
        }
    }

    private List<HydroRecord> parseResponse(String jsonBody, br.edu.floodstats.domain.enums.DataType dataType) {
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

            if (value != null) {
                dailyAverages.put(date, dailyAverages.getOrDefault(date, 0.0) + value);
                dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + 1);
            }
        }

        // Finaliza instanciando um HydroRecord por dia
        String unit = br.edu.floodstats.domain.enums.DataType.RAINFALL.equals(dataType) ? "mm" : "m³/s";
        for (java.util.Map.Entry<LocalDate, Double> entry : dailyAverages.entrySet()) {
            LocalDate date = entry.getKey();
            double avgValue = entry.getValue() / dailyCounts.get(date);

            if (br.edu.floodstats.domain.enums.DataType.RAINFALL.equals(dataType)) {
                records.add(new RainfallRecord(date, avgValue, unit, "EstacaoANA"));
            } else {
                records.add(new RiverDischargeRecord(date, avgValue, unit, "EstacaoANA"));
            }
        }

        return records;
    }

}
