package kz.uib.kitaphana.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    // Теңшеу үшін DB_URL, DB_USER, DB_PASSWORD орта айнымалыларын қолдануға болады.
    // Егер олар берілмесе, төмендегі жергілікті PostgreSQL параметрлері алынады.
    private static final String URL = setting("DB_URL",
            "jdbc:postgresql://localhost:5432/library_db");
    private static final String USER = setting("DB_USER", "postgres");
    private static final String PASSWORD = setting("DB_PASSWORD", "");

    private DBConnection() { }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
