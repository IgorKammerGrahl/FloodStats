package br.edu.floodstats.infrastructure.persistence;

import br.edu.floodstats.domain.gumbel.GumbelResult;
import br.edu.floodstats.domain.repository.GumbelRepository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteGumbelRepository implements GumbelRepository {
    private static final Logger LOGGER = Logger.getLogger(SqliteGumbelRepository.class.getName());
    private static final String DIR = "output/sqlite";
    private static final String URL = "jdbc:sqlite:output/sqlite/floodstats.db";

    public SqliteGumbelRepository() {
        try {
            Files.createDirectories(Paths.get(DIR));
            Connection conn = DriverManager.getConnection(URL);
            Statement stmt = conn.createStatement();
            String sql = """
                    CREATE TABLE IF NOT EXISTS gumbel_predictions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        station_code TEXT NOT NULL,
                        start_year INTEGER NOT NULL,
                        end_year INTEGER NOT NULL,
                        mean REAL NOT NULL,
                        standard_deviation REAL NOT NULL,
                        flow_10_years REAL NOT NULL,
                        flow_50_years REAL NOT NULL,
                        flow_100_years REAL NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP
                    );
                    """;
            stmt.execute(sql);
            conn.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erro ao inicializar tabela gumbel_predictions no SQLite", e);
        }
    }

    @Override
    public void save(GumbelResult result) throws Exception {
        String sql = "INSERT INTO gumbel_predictions (station_code, start_year, end_year, mean, standard_deviation, flow_10_years, flow_50_years, flow_100_years) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            pstmt.setString(1, result.getStationCode());
            pstmt.setInt(2, result.getStartYear());
            pstmt.setInt(3, result.getEndYear());
            pstmt.setDouble(4, result.getMean());
            pstmt.setDouble(5, result.getStandardDeviation());
            pstmt.setDouble(6, result.getFlow10Years());
            pstmt.setDouble(7, result.getFlow50Years());
            pstmt.setDouble(8, result.getFlow100Years());

            pstmt.executeUpdate();
            conn.commit();
        }
    }
}
