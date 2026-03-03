package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;
import java.util.List;
import java.util.UUID;
import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "floodStatsData")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataWrapper {
    private String analysisId;
    private String dataType;
    private String location;

    @XmlElementWrapper(name = "records")
    @XmlElements({
            @XmlElement(name = "rainfallRecord", type = RainfallRecord.class),
            @XmlElement(name = "riverDischargeRecord", type = RiverDischargeRecord.class)
    })
    private List<HydroRecord> records;

    public DataWrapper() {
    }

    public DataWrapper(String dataType, String location, List<HydroRecord> records) {
        this.analysisId = UUID.randomUUID().toString();
        this.dataType = dataType;
        this.location = location;
        this.records = records;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public String getDataType() {
        return dataType;
    }

    public String getLocation() {
        return location;
    }

    public List<HydroRecord> getRecords() {
        return records;
    }
}
