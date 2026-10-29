package dal;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Mỗi DAO mở một connection riêng và đóng bằng try-with-resources. */
public class DBContext implements AutoCloseable {

    protected final Connection connection;

    public DBContext() throws SQLException {
        String configPath = System.getProperty("keebforce.db.config");
        if (configPath == null || configPath.isBlank()) {
            configPath = System.getenv("KEEBFORCE_DB_CONFIG");
        }
        if (configPath == null || configPath.isBlank()) {
            // NetBeans/Tomcat: đọc file local theo vị trí cố định, không cần VM Option.
            String tomcatBase = System.getProperty("catalina.base");
            if (tomcatBase != null && !tomcatBase.isBlank()) {
                configPath = Path.of(tomcatBase, "conf", "keebforce-db.properties").toString();
            }
        }
        if (configPath == null || configPath.isBlank()) {
            throw new SQLException("Chưa cấu hình đường dẫn file DB: keebforce.db.config hoặc KEEBFORCE_DB_CONFIG.");
        }

        Properties config = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of(configPath), StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (IOException | IllegalArgumentException ex) {
            throw new SQLException("Không đọc được file cấu hình DB local.", ex);
        }

        String url = required(config, "db.url");
        if (!url.startsWith("jdbc:sqlserver://")) {
            throw new SQLException("db.url phải là JDBC URL của SQL Server.");
        }
        // Nạp rõ driver để dùng được cả trong Tomcat và chương trình kiểm tra console.
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Thiếu Microsoft JDBC Driver trong WEB-INF/lib.", ex);
        }
        Properties login = new Properties();
        if (Boolean.parseBoolean(config.getProperty("db.integratedSecurity", "false"))) {
            // Tùy chọn local trên Windows: driver cần DLL xác thực cùng phiên bản.
            login.setProperty("integratedSecurity", "true");
        } else {
            login.setProperty("user", required(config, "db.user"));
            login.setProperty("password", required(config, "db.password"));
        }
        login.setProperty("loginTimeout", "5");
        connection = DriverManager.getConnection(url, login);
    }

    private static String required(Properties config, String key) throws SQLException {
        String value = config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SQLException("Thiếu cấu hình " + key + ".");
        }
        return value;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
