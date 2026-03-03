package br.edu.floodstats.infrastructure.factory;

import br.edu.floodstats.domain.enums.PersistenceFormat;
import br.edu.floodstats.infrastructure.persistence.DataPersistence;
import br.edu.floodstats.infrastructure.persistence.JsonPersistence;
import br.edu.floodstats.infrastructure.persistence.SqlitePersistence;
import br.edu.floodstats.infrastructure.persistence.XmlPersistence;

public class PersistenceFactory {
    public DataPersistence create(PersistenceFormat format) {
        switch (format) {
            case JSON:
                return new JsonPersistence();
            case XML:
                return new XmlPersistence();
            case SQLITE:
                return new SqlitePersistence();
            default:
                throw new IllegalArgumentException("Formato de persistência não suportado: " + format);
        }
    }
}
