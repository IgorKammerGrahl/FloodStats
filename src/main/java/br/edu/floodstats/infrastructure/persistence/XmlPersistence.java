package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XmlPersistence implements DataPersistence {

    @Override
    public void save(List<HydroRecord> records, String fileName) throws Exception {
        if (records == null || records.isEmpty())
            return;

        String dataType = records.get(0) instanceof RainfallRecord ? "RAINFALL" : "RIVER_DISCHARGE";
        String location = records.get(0).getLocationName();
        DataWrapper wrapper = new DataWrapper(dataType, location, records);

        JAXBContext context = JAXBContext.newInstance(DataWrapper.class, RainfallRecord.class,
                RiverDischargeRecord.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        marshaller.marshal(wrapper, new File(fileName));
    }

    @Override
    public List<HydroRecord> load(String fileName) throws Exception {
        JAXBContext context = JAXBContext.newInstance(DataWrapper.class, RainfallRecord.class,
                RiverDischargeRecord.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        DataWrapper wrapper = (DataWrapper) unmarshaller.unmarshal(new File(fileName));
        return wrapper.getRecords() != null ? wrapper.getRecords() : new ArrayList<>();
    }
}
