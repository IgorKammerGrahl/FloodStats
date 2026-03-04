package br.edu.floodstats.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiConfig {
    private final Properties properties;

    public ApiConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            System.out.println("[DEBUG] Carregando credenciais do properties...");
            if (input == null) {
                System.err.println("Aviso: arquivo application.properties não encontrado. Usando valores padrão.");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            System.err.println("Erro ao carregar o application.properties: " + ex.getMessage());
        }
    }

    public String getAnaApiUrl() {
        return properties.getProperty("ana.api.url", "https://www.snirh.gov.br/hidroweb/rest/api");
    }

    public String getAnaApiIdentificador() {
        return properties.getProperty("ana.api.identificador", "");
    }

    public String getAnaApiSenha() {
        return properties.getProperty("ana.api.senha", "");
    }
}
