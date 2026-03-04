package br.edu.floodstats.infrastructure.api;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AnaApiIntegrationTest {

    @Test
    public void testAnaApiUrls() throws Exception {
        System.out.println("=== INICIANDO TESTE EXPLORATÓRIO ANA API ===");
        AnaTokenService tokenService = new AnaTokenService();
        String token = tokenService.getToken();
        System.out.println("Token obtido: " + (token != null ? "Sim" : "Não"));

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String anaBaseAdotada = "https://www.ana.gov.br/hidrowebservice/EstacoesTelemetricas/HidroinfoanaSerieTelemetricaAdotada/v1";

        String codEstParam = URLEncoder.encode("Código da Estação", StandardCharsets.UTF_8.toString());
        String tipoFiltroParam = URLEncoder.encode("Tipo Filtro Data", StandardCharsets.UTF_8.toString());
        String dataBuscaParam = URLEncoder.encode("Data de Busca (yyyy-MM-dd)", StandardCharsets.UTF_8.toString());
        String rangeParam = URLEncoder.encode("Range Intervalo de busca", StandardCharsets.UTF_8.toString());

        String url1 = anaBaseAdotada + "?" + codEstParam + "=15400000&" + tipoFiltroParam + "=DATA_LEITURA&"
                + rangeParam + "=DIAS_30";
        String url2 = anaBaseAdotada + "?" + codEstParam + "=15400000&" + tipoFiltroParam + "=DATA_LEITURA&"
                + dataBuscaParam + "=2026-02-01&" + rangeParam + "=DIAS_30";

        String dataInicioParam = URLEncoder.encode("Data Inicial (yyyy-MM-dd)", StandardCharsets.UTF_8.toString());
        String dataFimParam = URLEncoder.encode("Data Final (yyyy-MM-dd)", StandardCharsets.UTF_8.toString());
        String anaBaseVazao = "https://www.ana.gov.br/hidrowebservice/EstacoesTelemetricas/HidroSerieVazao/v1";

        String dateInicio = "2025-01-01T00:00:00.000Z";
        String encodedInicio = URLEncoder.encode(dateInicio, StandardCharsets.UTF_8.toString());

        String dateFim = "2025-12-31T00:00:00.000Z";
        String encodedFim = URLEncoder.encode(dateFim, StandardCharsets.UTF_8.toString());

        String url3 = anaBaseVazao + "?" + codEstParam + "=15400000&" + tipoFiltroParam + "=DATA_LEITURA&"
                + dataInicioParam + "=" + encodedInicio + "&" + dataFimParam + "=" + encodedFim;
        String url4 = anaBaseVazao + "?" + codEstParam + "=15400000&" + tipoFiltroParam + "=DATA_LEITURA&"
                + dataInicioParam + "=2025-01-01&" + dataFimParam + "=2025-12-31";

        testUrl("Adotada Sem Data", url1, token, client);
        testUrl("Adotada Com Data", url2, token, client);
        testUrl("Vazao Convencional ISO", url3, token, client);
        testUrl("Vazao Convencional BR", url4, token, client);

        System.out.println("=== FIM DO TESTE EXPLORATÓRIO ANA API ===");
    }

    private void testUrl(String name, String url, String token, HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(name + " retornou " + response.statusCode());
            if (response.statusCode() != 200) {
                System.out.println("   Response Body: " + response.body());
            } else {
                if (response.body().length() > 200) {
                    System.out.println("   Response Body preview: " + response.body().substring(0, 200) + "...");
                } else {
                    System.out.println("   Response Body: " + response.body());
                }
            }
        } catch (Exception e) {
            System.out.println(name + " falhou com exceção: " + e.getMessage());
        }
    }
}
