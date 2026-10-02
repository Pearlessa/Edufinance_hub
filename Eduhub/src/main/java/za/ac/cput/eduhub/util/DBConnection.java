package za.ac.cput.eduhub.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String CLIENT_URL = "jdbc:derby://localhost:1527/edufinancehub_db;create=true";
    private static final String EMBEDDED_URL = "jdbc:derby:" + System.getProperty("user.home") + "/.eduhub_local_db;create=true";
    private static final String DB_USER = "app";
    private static final String DB_PASS = "app";

    static {
        try {
            Class.forName("org.apache.derby.client.ClientAutoloadedDriver");
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("org.apache.derby.jdbc.ClientDriver");
            } catch (ClassNotFoundException e2) {
                try {
                    Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
                } catch (ClassNotFoundException e3) {
                    System.err.println("[DBConnection] Warning: Derby driver not loaded: " + e3.getMessage());
                }
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn;
        try {
            conn = DriverManager.getConnection(CLIENT_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            conn = DriverManager.getConnection(EMBEDDED_URL);
        }
        ensureUsersTableExists(conn);
        return conn;
    }

    private static void ensureUsersTableExists(Connection conn) {
        String sql = "CREATE TABLE APP.APP_USERS (" +
                     "  user_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY," +
                     "  full_name VARCHAR(120) NOT NULL," +
                     "  email VARCHAR(150) NOT NULL UNIQUE," +
                     "  phone_number VARCHAR(25) NOT NULL," +
                     "  student_type VARCHAR(60) NOT NULL," +
                     "  password_hash VARCHAR(255) NOT NULL," +
                     "  failed_attempts INT DEFAULT 0," +
                     "  is_locked BOOLEAN DEFAULT FALSE," +
                     "  profile_completed BOOLEAN DEFAULT FALSE," +
                     "  xp_points INT DEFAULT 0," +
                     "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ignored) {
         
        }
    }
}