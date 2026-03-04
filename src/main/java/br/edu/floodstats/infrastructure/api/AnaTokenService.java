package br.edu.floodstats.infrastructure.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import br.edu.floodstats.infrastructure.config.ApiConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AnaTokenService {
    private static final String CACHE_FILE_NAME = "ana_token_cache.properties";
    private static final long MAX_AGE_MILLIS = 55 * 60 * 1000; // 55 minutos

    private final HttpClient httpClient;
    private final ApiConfig apiConfig;

    public AnaTokenService() {
        this.apiConfig = new ApiConfig();
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
        File file = new File(CACHE_FILE_NAME);
        if (!file.exists()) {
            return null;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);

            String token = props.getProperty("token");
            String timestampStr = props.getProperty("timestamp");

            if (token != null && timestampStr != null) {
                long timestamp = Long.parseLong(timestampStr);
                if (System.currentTimeMillis() - timestamp < MAX_AGE_MILLIS) {
                    return token;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Aviso: Falha ao ler cache do token da ANA: " + e.getMessage());
        }
        return null;
    }

    private void saveTokenToCache(String token) {
        Properties props = new Properties();
        props.setProperty("token", token);
        props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));

        try (FileOutputStream out = new FileOutputStream(CACHE_FILE_NAME)) {
            props.store(out, "ANA API Token Cache");
        } catch (IOException e) {
            System.err.println("Aviso: Falha ao gravar cache do token da ANA: " + e.getMessage());
        }
    }
}
