package br.edu.floodstats.infrastructure.api;

import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenMeteoFloodFetcher implements DataFetcher {
    private static final String BASE_URL = "https://flood-api.open-meteo.com/v1/flood";
    private final HttpClient httpClient;
    private final Gson gson;

    public OpenMeteoFloodFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.gson = new Gson();
    }

    @Override
    public List<HydroRecord> fetch(AnalysisRequest request) throws DataFetchException {
        List<HydroRecord> records = fetchWithCoords(request.getLatitude(), request.getLongitude(), request);

        double avg = records.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);

        if (!records.isEmpty() && avg < 10.0) {
            double shiftedLon = request.getLongitude() + 0.1;
            List<HydroRecord> shiftedRecords = fetchWithCoords(request.getLatitude(), shiftedLon, request);

            double shiftedAvg = shiftedRecords.stream().mapToDouble(HydroRecord::getValue).average().orElse(0.0);

            if (shiftedAvg > avg) {
                return shiftedRecords;
            }
        }

        return records;
    }

    private List<HydroRecord> fetchWithCoords(double latitude, double longitude, AnalysisRequest request)
            throws DataFetchException {
        String urlString = String.format(Locale.US,
                "%s?latitude=%.6f&longitude=%.6f&daily=river_discharge&start_date=%s&end_date=%s",
                BASE_URL, latitude, longitude,
                request.getStartDate().toString(), request.getEndDate().toString());

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new DataFetchException("Falha na comunicação com a API de Enchentes", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataFetchException("Falha na comunicação com a API de Enchentes", e);
        }

        if (response.statusCode() != 200) {
            throw new DataFetchException("Erro ao buscar dados da API de Enchentes: HTTP " + response.statusCode());
        }

        return parseJson(response.body(), "Lat: " + latitude + ", Lon: " + longitude);
    }

    private List<HydroRecord> parseJson(String jsonBody, String locationName) {
        List<HydroRecord> records = new ArrayList<>();
        JsonObject jsonObject = gson.fromJson(jsonBody, JsonObject.class);

        if (!jsonObject.has("daily"))
            return records;

        JsonObject daily = jsonObject.getAsJsonObject("daily");
        JsonArray timeArray = daily.getAsJsonArray("time");
        JsonArray dischargeArray = daily.getAsJsonArray("river_discharge");

        String unit = "m³/s"; // Default based on API
        if (jsonObject.has("daily_units")) {
            unit = jsonObject.getAsJsonObject("daily_units").get("river_discharge").getAsString();
        }

        for (int i = 0; i < timeArray.size(); i++) {
            LocalDate date = LocalDate.parse(timeArray.get(i).getAsString());
            JsonElement dischargeElement = dischargeArray.get(i);

            double value = 0.0;
            if (!dischargeElement.isJsonNull()) {
                value = dischargeElement.getAsDouble();
            }

            records.add(new RiverDischargeRecord(date, value, unit, locationName));
        }

        return records;
    }
}
