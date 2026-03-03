@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(type = LocalDate.class, value = br.edu.floodstats.infrastructure.persistence.LocalDateAdapter.class)
})
package br.edu.floodstats.domain.model;

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.LocalDate;
