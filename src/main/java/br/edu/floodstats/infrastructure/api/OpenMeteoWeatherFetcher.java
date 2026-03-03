package br.edu.floodstats.infrastructure.api;

import br.edu.floodstats.domain.model.AnalysisRequest;
import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
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

public class OpenMeteoWeatherFetcher implements DataFetcher {
    private static final String BASE_URL = "https://archive-api.open-meteo.com/v1/archive";
    private final HttpClient httpClient;
    private final Gson gson;

    public OpenMeteoWeatherFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.gson = new Gson();
    }

    @Override
    public List<HydroRecord> fetch(AnalysisRequest request) throws DataFetchException {
        String urlString = String.format(Locale.US,
                "%s?latitude=%.6f&longitude=%.6f&start_date=%s&end_date=%s&daily=precipitation_sum&timezone=America/Sao_Paulo",
                BASE_URL, request.getLatitude(), request.getLongitude(),
                request.getStartDate().toString(), request.getEndDate().toString());

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new DataFetchException("Falha na comunicação com a API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataFetchException("Falha na comunicação com a API", e);
        }

        if (response.statusCode() != 200) {
            throw new DataFetchException("Erro ao buscar dados da API: HTTP " + response.statusCode());
        }

        return parseJson(response.body(), "Lat: " + request.getLatitude() + ", Lon: " + request.getLongitude());
    }

    private List<HydroRecord> parseJson(String jsonBody, String locationName) {
        List<HydroRecord> records = new ArrayList<>();
        JsonObject jsonObject = gson.fromJson(jsonBody, JsonObject.class);

        if (!jsonObject.has("daily"))
            return records;

        JsonObject daily = jsonObject.getAsJsonObject("daily");
        JsonArray timeArray = daily.getAsJsonArray("time");
        JsonArray precipArray = daily.getAsJsonArray("precipitation_sum");

        String unit = "mm"; // Default based on API
        if (jsonObject.has("daily_units")) {
            unit = jsonObject.getAsJsonObject("daily_units").get("precipitation_sum").getAsString();
        }

        for (int i = 0; i < timeArray.size(); i++) {
            LocalDate date = LocalDate.parse(timeArray.get(i).getAsString());
            JsonElement precipElement = precipArray.get(i);

            double value = 0.0;
            if (!precipElement.isJsonNull()) {
                value = precipElement.getAsDouble();
            }

            records.add(new RainfallRecord(date, value, unit, locationName));
        }

        return records;
    }
}
