package br.edu.floodstats.application;

import br.edu.floodstats.domain.model.HydroRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrossValidationService {

    /**
     * Valida cross-referenced data arrays para calcular o Coeficiente de
     * Nash-Sutcliffe (NSE).
     * O NSE mede o quanto a predição (simulated) é melhor na identificação de
     * variâncias em
     * relação à média das observações (observed).
     *
     * @param simulated Reanálises/Simulações (ex: Open-Meteo)
     * @param observed  Leituras Observadas (ex: ANA)
     * @return O NSE ou NaN caso os dados sejam inválidos ou causem divisão por
     *         zero.
     */
    public double mergeAndValidate(List<HydroRecord> simulated, List<HydroRecord> observed) {
        // Alinhamos os dados em mapas temporais, pois nem tudo que tem na série
        // simulada pode ter na observada
        Map<String, Double> observedMap = new HashMap<>(); // chave: Date + Tipo
        for (HydroRecord obs : observed) {
            String key = obs.getDate().toString() + "_" + obs.getDataTypeName();
            observedMap.put(key, obs.getValue());
        }

        List<Double> alignedSimulated = new ArrayList<>();
        List<Double> alignedObserved = new ArrayList<>();

        for (HydroRecord sim : simulated) {
            String key = sim.getDate().toString() + "_" + sim.getDataTypeName();
            if (observedMap.containsKey(key)) {
                alignedSimulated.add(sim.getValue());
                alignedObserved.add(observedMap.get(key));
            }
        }

        return calculateNSE(alignedSimulated, alignedObserved);
    }

    /**
     * Calcula o Nash-Sutcliffe Efficiency (NSE)
     * Formula: NSE = 1 - [ ∑(Obs_i - Sim_i)² / ∑(Obs_i - MédiaObs)² ]
     */
    public double calculateNSE(List<Double> simulatedValues, List<Double> observedValues) {
        if (simulatedValues == null || observedValues == null ||
                simulatedValues.isEmpty() || observedValues.isEmpty() ||
                simulatedValues.size() != observedValues.size()) {
            return Double.NaN;
        }

        int n = observedValues.size();

        // Média dos valores observados (Mean of observed)
        double sumObs = 0.0;
        for (Double obs : observedValues) {
            sumObs += obs;
        }
        double meanObs = sumObs / n;

        double numerator = 0.0; // Somatório do erro simulação x observação (Residual sum of squares)
        double denominator = 0.0; // Variância dos dados observados em relação à média

        for (int i = 0; i < n; i++) {
            double sim = simulatedValues.get(i);
            double obs = observedValues.get(i);

            numerator += Math.pow(obs - sim, 2);
            denominator += Math.pow(obs - meanObs, 2);
        }

        // Previne divisão por zero (se todos as observações forem idênticas à média,
        // variância = 0)
        if (denominator == 0.0) {
            // Se o denominador = 0, a perfomance depende apenas do numerador
            if (numerator == 0.0) {
                return 1.0; // Perfeito, caso extremo em que Obs=Media e Sim=Media todo o tempo
            }
            return Double.NaN;
        }

        return 1.0 - (numerator / denominator);
    }
}
