package br.edu.floodstats.infrastructure.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import br.edu.floodstats.infrastructure.config.ApiConfig;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AnaTokenService {
    private static final String CACHE_FILE_PATH = System.getProperty("user.home") + "/.floodstats_ana_token.json";
    private static final long MAX_AGE_MILLIS = 55L * 60 * 1000; // 55 minutes

    private record TokenCache(String token, long expirationTimeMillis) {
    }

    private final HttpClient httpClient;
    private final ApiConfig apiConfig;
    private final Gson gson;

    public AnaTokenService() {
        this.apiConfig = new ApiConfig();
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public synchronized String getToken() throws Exception {
        String cachedToken = loadCachedToken();
        if (cachedToken != null) {
            return cachedToken;
        }

        String identificador = apiConfig.getAnaApiIdentificador();
        if (identificador != null) {
            identificador = identificador.trim();
        } else {
            identificador = "";
        }

        String senha = apiConfig.getAnaApiSenha();
        if (senha != null) {
            senha = senha.trim();
        } else {
            senha = "";
        }

        System.out.println("[DEBUG] Identificador a ser enviado: [" + identificador + "]");
        System.out.println("[DEBUG] Senha a ser enviada: [" + senha + "]");

        // URL obrigatória do manual (diferente da base para outras requisições)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.ana.gov.br/hidrowebservice/EstacoesTelemetricas/OAUth/v1"))
                .header("Identificador", identificador)
                .header("Senha", senha)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            try {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject itemsObj = jsonResponse.getAsJsonObject("items");
                String newToken = itemsObj.get("tokenautenticacao").getAsString();

                saveTokenToCache(newToken);
                return newToken;
            } catch (Exception e) {
                throw new RuntimeException("Falha ao processar resposta da ANA (JSON inesperado): " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Falha ao obter token da ANA. Status: " + response.statusCode());
        }
    }

    private String loadCachedToken() {
        File file = new File(CACHE_FILE_PATH);
        if (!file.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            TokenCache cache = gson.fromJson(reader, TokenCache.class);
            if (cache != null && System.currentTimeMillis() < cache.expirationTimeMillis()) {
                System.out.println("[INFO] Usando token da ANA existente do cache local.");
                return cache.token();
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao ler cache do token JSON da ANA: " + e.getMessage());
        }
        return null;
    }

    private void saveTokenToCache(String token) {
        long expiration = System.currentTimeMillis() + MAX_AGE_MILLIS;
        TokenCache cache = new TokenCache(token, expiration);

        try (FileWriter writer = new FileWriter(CACHE_FILE_PATH)) {
            gson.toJson(cache, writer);
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao gravar cache JSON do token da ANA: " + e.getMessage());
        }
    }
}
