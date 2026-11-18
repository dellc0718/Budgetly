package budgetly.database;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central place for the Apache Derby (Java DB) Client connection settings.
 * Every DAO asks this class for a JDBC {@link Connection}.
 */
public final class DatabaseConnection {

    public static final String CLIENT_DRIVER = "org.apache.derby.client.ClientAutoloadedDriver";
    public static final String URL = "jdbc:derby://localhost:1527/BudgetlyDB;create=true";
    private static final String DB_USER = "budgetly";
    private static final String DB_PASSWORD = "budgetly";
    private static final int PORT = 1527;

    private DatabaseConnection() {
        // utility class: no objects needed
    }

    /**
     * Opens a new connection to the Apache Derby Network Server using the Derby Client driver.
     * The caller must close it (we always use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        startNetworkServerIfNeeded();
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Creates the tables from database/schema.sql if they do not exist yet.
     */
    public static void initializeDatabase() throws SQLException, IOException {
        try (Connection conn = getConnection()) {
            if (tableExists(conn, "USERS")) {
                return;
            }
            File schemaFile = findSchemaFile();
            String script = removeComments(
                    new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8));
            try (Statement stmt = conn.createStatement()) {
                for (String sql : script.split(";")) {
                    String cleaned = sql.trim();
                    if (!cleaned.isEmpty()) {
                        stmt.executeUpdate(cleaned);
                    }
                }
            }
        }
    }

    /**
     * Ensures the Apache Derby Network Server is listening on localhost:1527
     * so the Derby Client driver can connect even if the server was not started manually.
     */
    private static synchronized void startNetworkServerIfNeeded() {
        if (isPortOpen()) {
            return;
        }
        try {
            Class<?> serverClass = Class.forName("org.apache.derby.drda.NetworkServerControl");
            Object server = serverClass
                    .getConstructor(InetAddress.class, int.class)
                    .newInstance(InetAddress.getByName("localhost"), PORT);
            serverClass.getMethod("start", PrintWriter.class).invoke(server, (PrintWriter) null);
            for (int i = 0; i < 25 && !isPortOpen(); i++) {
                Thread.sleep(100);
            }
        } catch (Exception ignored) {
            // If derbynet.jar is not on the classpath, DriverManager will report the connection error.
        }
    }

    private static boolean isPortOpen() {
        try (Socket socket = new Socket("localhost", PORT)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File findSchemaFile() {
        File direct = new File("database/schema.sql");
        if (direct.exists()) {
            return direct;
        }
        return new File("Budgetly/database/schema.sql");
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, DB_USER.toUpperCase(), tableName, null)) {
            return rs.next();
        }
    }

    private static String removeComments(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                result.append(line).append('\n');
            }
        }
        return result.toString().trim();
    }
}
