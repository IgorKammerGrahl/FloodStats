package br.edu.floodstats.domain.repository;

import br.edu.floodstats.domain.gumbel.GumbelResult;

public interface GumbelRepository {
    void save(GumbelResult result) throws Exception;
}
