package br.edu.floodstats.application;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrossValidationServiceTest {

    private CrossValidationService service;

    @BeforeEach
    void setUp() {
        service = new CrossValidationService();
    }

    @Test
    void testCalculateNSE_PerfectMatch() {
        List<Double> simulated = Arrays.asList(10.0, 20.0, 30.0);
        List<Double> observed = Arrays.asList(10.0, 20.0, 30.0);

        double nse = service.calculateNSE(simulated, observed);
        assertEquals(1.0, nse, 0.001, "Para valores perfeitamente idênticos, o NSE deve ser 1.0");
    }

    @Test
    void testCalculateNSE_AveragePerformance() {
        // Observado Média = 20
        List<Double> observed = Arrays.asList(10.0, 20.0, 30.0);
        // Numerador: (10-15)^2 + (20-15)^2 + (30-35)^2 = 25 + 25 + 25 = 75
        // Denominador: (10-20)^2 + (20-20)^2 + (30-20)^2 = 100 + 0 + 100 = 200
        // NSE: 1 - (75/200) = 1 - 0.375 = 0.625
        List<Double> simulated = Arrays.asList(15.0, 15.0, 35.0);

        double nse = service.calculateNSE(simulated, observed);
        assertEquals(0.625, nse, 0.001);
    }

    @Test
    void testCalculateNSE_ReturnsNaNForDivisionByZero() {
        // Média = 10
        // Denominador = 0, pois todas as observações são 10
        List<Double> observed = Arrays.asList(10.0, 10.0, 10.0);
        List<Double> simulated = Arrays.asList(12.0, 15.0, 18.0);

        double nse = service.calculateNSE(simulated, observed);
        assertTrue(Double.isNaN(nse),
                "Se o denominador da fórumla for 0 (sem variação nas observações), NSE deve ser NaN");
    }

    @Test
    void testCalculateNSE_ReturnsNaNForEmptyLists() {
        double nse = service.calculateNSE(Collections.emptyList(), Collections.emptyList());
        assertTrue(Double.isNaN(nse));
    }

    @Test
    void testMergeAndValidate_AlignsDataByDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        List<HydroRecord> observed = Arrays.asList(
                new RainfallRecord(yesterday, 10.0, "mm", "A"),
                new RainfallRecord(today, 20.0, "mm", "A")
        // Amanhã não foi observado ainda
        );

        List<HydroRecord> simulated = Arrays.asList(
                new RainfallRecord(yesterday, 12.0, "mm", "A"),
                new RainfallRecord(today, 18.0, "mm", "A"),
                new RainfallRecord(tomorrow, 50.0, "mm", "A") // Simulação projeta amanhã
        );

        // Somente yesterday e today coincidem.
        // Observado Alinhado: 10, 20 (Média 15)
        // Simulado Alinhado: 12, 18
        // Numerador: (10-12)^2 + (20-18)^2 = 4 + 4 = 8
        // Denominador: (10-15)^2 + (20-15)^2 = 25 + 25 = 50
        // NSE: 1 - (8/50) = 1 - 0.16 = 0.84

        double nse = service.mergeAndValidate(simulated, observed);
        assertEquals(0.84, nse, 0.001);
    }
}
