package budgetlyDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides database health checks, Derby metadata diagnostics, schema verification,
 * and sanitized error monitoring for the administrator Database Management console.
 */
public class DatabaseManager {

    private final AdminDAO adminDAO = new AdminDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static final class DatabaseStatusInfo {
        private final boolean connected;
        private final String productName;
        private final String productVersion;
        private final String driverName;
        private final String driverVersion;
        private final String jdbcUrl;
        private final String schemaName;
        private final String databaseUser;
        private final int pooledConnections;
        private final int totalCreatedConnections;
        private final int tableCount;
        private final String initSummary;

        public DatabaseStatusInfo(boolean connected, String productName, String productVersion,
                                  String driverName, String driverVersion, String jdbcUrl,
                                  String schemaName, String databaseUser, int pooledConnections,
                                  int totalCreatedConnections, int tableCount, String initSummary) {
            this.connected = connected;
            this.productName = productName;
            this.productVersion = productVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.jdbcUrl = jdbcUrl;
            this.schemaName = schemaName;
            this.databaseUser = databaseUser;
            this.pooledConnections = pooledConnections;
            this.totalCreatedConnections = totalCreatedConnections;
            this.tableCount = tableCount;
            this.initSummary = initSummary;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getProductName() {
            return productName;
        }

        public String getProductVersion() {
            return productVersion;
        }

        public String getDriverName() {
            return driverName;
        }

        public String getDriverVersion() {
            return driverVersion;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public String getDatabaseUser() {
            return databaseUser;
        }

        public int getPooledConnections() {
            return pooledConnections;
        }

        public int getTotalCreatedConnections() {
            return totalCreatedConnections;
        }

        public int getTableCount() {
            return tableCount;
        }

        public String getInitSummary() {
            return initSummary;
        }
    }

    public DatabaseStatusInfo getDatabaseStatus() {
        SessionManager.requireAdmin();
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            List<String> tables = adminDAO.listApplicationTables();
            return new DatabaseStatusInfo(
                    !con.isClosed() && con.isValid(2),
                    meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion(),
                    meta.getDriverName(),
                    meta.getDriverVersion(),
                    DatabaseConnection.DB_URL,
                    DatabaseConnection.SCHEMA_NAME,
                    DatabaseConnection.getDatabaseUser(),
                    DatabaseConnection.getPooledConnectionCount(),
                    DatabaseConnection.getTotalCreatedConnectionCount(),
                    tables.size(),
                    DatabaseInitializer.getLastInitializationMessage()
            );
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Database Status Check", ex);
            return new DatabaseStatusInfo(
                    false,
                    "Apache Derby (Unavailable)",
                    "-",
                    "-",
                    "-",
                    DatabaseConnection.DB_URL,
                    DatabaseConnection.SCHEMA_NAME,
                    DatabaseConnection.getDatabaseUser(),
                    DatabaseConnection.getPooledConnectionCount(),
                    DatabaseConnection.getTotalCreatedConnectionCount(),
                    0,
                    "Error: " + ex.getMessage()
            );
        }
    }

    public String verifySchemaIntegrity() throws SQLException {
        SessionManager.requireAdmin();
        DatabaseInitializer.initialize();
        List<String> tables = adminDAO.listApplicationTables();
        String msg = "Verified " + tables.size() + " application tables in schema "
                + DatabaseConnection.SCHEMA_NAME + ".";
        activityLogDAO.logActivity(
                SessionManager.getCurrentUserId(),
                SessionManager.getCurrentUsername(),
                "DB_MAINTENANCE",
                msg
        );
        return msg;
    }

    public List<DatabaseConnection.DatabaseErrorRecord> getRecentErrors() {
        SessionManager.requireAdmin();
        return DatabaseConnection.getRecentErrors();
    }

    public void clearErrorLog() {
        SessionManager.requireAdmin();
        DatabaseConnection.clearRecordedErrors();
        activityLogDAO.logActivity(
                SessionManager.getCurrentUserId(),
                SessionManager.getCurrentUsername(),
                "DB_MAINTENANCE",
                "Cleared in-memory database error monitor log."
        );
    }
}
