package br.edu.floodstats.infrastructure.factory;

import br.edu.floodstats.domain.enums.DataType;
import br.edu.floodstats.infrastructure.api.DataFetcher;
import br.edu.floodstats.infrastructure.api.OpenMeteoFloodFetcher;
import br.edu.floodstats.infrastructure.api.OpenMeteoWeatherFetcher;

import br.edu.floodstats.infrastructure.api.AnaHidroWebFetcher;
import br.edu.floodstats.infrastructure.api.AnaTokenService;
import java.util.List;
import java.util.Arrays;

public class DataFetcherFactory {
    public DataFetcher create(DataType dataType) {
        switch (dataType) {
            case RAINFALL:
                return new OpenMeteoWeatherFetcher();
            case RIVER_DISCHARGE:
                return new OpenMeteoFloodFetcher();
            // A API da ANA fornece ambos, a decisão de como usá-la individualmente
            // depende de regras de negócio, mas ela será injetada para todos os dados no
            // allFetchers
            default:
                throw new IllegalArgumentException("Tipo de dados não suportado para fetcher individual: " + dataType);
        }
    }

    public List<DataFetcher> createAllFetchers() {
        // Serviços instanciados de acordo com os princípios de injeção de dependência
        // manual (Factory Pattern)
        AnaTokenService tokenService = new AnaTokenService();
        AnaHidroWebFetcher anaFetcher = new AnaHidroWebFetcher(tokenService);

        return Arrays.asList(
                new OpenMeteoWeatherFetcher(),
                new OpenMeteoFloodFetcher(),
                anaFetcher);
    }
}
