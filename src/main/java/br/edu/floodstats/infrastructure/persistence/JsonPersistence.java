package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JsonPersistence implements DataPersistence {
    private final Gson gson;

    public JsonPersistence() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void save(List<HydroRecord> records, String fileName) throws Exception {
        if (records == null || records.isEmpty())
            return;

        String dataType = records.get(0) instanceof RainfallRecord ? "RAINFALL" : "RIVER_DISCHARGE";
        String location = records.get(0).getLocationName();
        DataWrapper wrapper = new DataWrapper(dataType, location, records);

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(wrapper, writer);
        }
    }

    @Override
    public List<HydroRecord> load(String fileName) throws Exception {
        try (FileReader reader = new FileReader(fileName)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            String dataType = jsonObject.get("dataType").getAsString();
            JsonElement recordsElement = jsonObject.get("records");

            List<HydroRecord> targetList = new ArrayList<>();
            if ("RAINFALL".equals(dataType)) {
                Type listType = new TypeToken<List<RainfallRecord>>() {
                }.getType();
                List<RainfallRecord> list = gson.fromJson(recordsElement, listType);
                targetList.addAll(list);
            } else {
                Type listType = new TypeToken<List<RiverDischargeRecord>>() {
                }.getType();
                List<RiverDischargeRecord> list = gson.fromJson(recordsElement, listType);
                targetList.addAll(list);
            }
            return targetList;
        }
    }
}
