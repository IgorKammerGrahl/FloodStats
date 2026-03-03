package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.model.HydroRecord;
import br.edu.floodstats.domain.model.RainfallRecord;
import br.edu.floodstats.domain.model.RiverDischargeRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlitePersistence implements DataPersistence {
    private static final Logger LOGGER = Logger.getLogger(SqlitePersistence.class.getName());
    private static final String URL = "jdbc:sqlite:floodstats.db";

    public SqlitePersistence() {
        try (Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement()) {

            String sql = """
                    CREATE TABLE IF NOT EXISTS hydro_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        analysis_id TEXT NOT NULL,
                        record_date TEXT NOT NULL,
                        value REAL NOT NULL,
                        unit TEXT NOT NULL,
                        data_type TEXT NOT NULL,
                        location_name TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    );
                    """;
            stmt.execute(sql);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao inicializar banco SQLite", e);
        }
    }

    @Override
    public void save(List<HydroRecord> records, String fileName) throws Exception {
        if (records == null || records.isEmpty())
            return;

        // Treat fileName as an analysis identifier, or generate one if not provided
        // appropriately
        String analysisId = (fileName != null && !fileName.isEmpty() && !fileName.equals("sqlite_database"))
                ? fileName
                : UUID.randomUUID().toString();

        String dataType = records.get(0) instanceof RainfallRecord ? "RAINFALL" : "RIVER_DISCHARGE";

        String sql = "INSERT INTO hydro_records (analysis_id, record_date, value, unit, data_type, location_name) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            pstmt.setString(1, analysisId);
            pstmt.setString(5, dataType);

            for (HydroRecord hydroRecord : records) {
                pstmt.setString(2, hydroRecord.getDate().toString());
                pstmt.setDouble(3, hydroRecord.getValue());
                pstmt.setString(4, hydroRecord.getUnit());
                pstmt.setString(6, hydroRecord.getLocationName());
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();

            LOGGER.log(Level.INFO, "Dados salvos no SQLite com Analysis ID (copie para carregar depois): {0}",
                    analysisId);
        }
    }

    @Override
    public List<HydroRecord> load(String analysisIdStr) throws Exception {
        List<HydroRecord> records = new ArrayList<>();
        String sql = "SELECT id, analysis_id, record_date, value, unit, data_type, location_name FROM hydro_records WHERE analysis_id = ? ORDER BY record_date ASC";

        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, analysisIdStr);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                LocalDate date = LocalDate.parse(rs.getString("record_date"));
                double value = rs.getDouble("value");
                String unit = rs.getString("unit");
                String dataType = rs.getString("data_type");
                String locationName = rs.getString("location_name");

                if ("RAINFALL".equals(dataType)) {
                    records.add(new RainfallRecord(date, value, unit, locationName));
                } else {
                    records.add(new RiverDischargeRecord(date, value, unit, locationName));
                }
            }
        }
        return records;
    }
}
