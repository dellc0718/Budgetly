package budgetlyDB;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized JDBC connection manager for Apache Derby (Java DB).
 *
 * <p>Reuses a small bounded pool of physical connections to {@code AdminDB} so callers
 * can safely use try-with-resources without repeatedly creating TCP connections or colliding
 * on transaction {@code autoCommit} state.</p>
 */
public final class DatabaseConnection {

    public static final String HOST = "localhost";
    public static final int PORT = 1527;
    public static final String DATABASE_NAME = "AdminDB";
    public static final String SCHEMA_NAME = "ADMINUSER";
    public static final String DB_URL = "jdbc:derby://" + HOST + ":" + PORT + "/" + DATABASE_NAME;
    private static final String DB_USER = "adminUser";
    private static final String DB_PASSWORD = "admin";

    private static final int MAX_POOL_SIZE = 4;
    private static final int MAX_ERROR_LOG_SIZE = 100;

    private static final ConcurrentLinkedDeque<Connection> POOL = new ConcurrentLinkedDeque<>();
    private static final List<Connection> ALL_PHYSICAL_CONNECTIONS = Collections.synchronizedList(new ArrayList<>());
    private static final List<DatabaseErrorRecord> ERROR_HISTORY = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);
    private static volatile boolean embeddedServerStartedByApp = false;
    private static volatile Object managedServerControl = null;

    private DatabaseConnection() {
    }

    /**
     * Sanitized database error entry for administrator monitoring.
     * Never stores credentials or sensitive user secrets.
     */
    public static final class DatabaseErrorRecord {
        private final Timestamp timestamp;
        private final String context;
        private final String sqlState;
        private final String message;

        public DatabaseErrorRecord(Timestamp timestamp, String context, String sqlState, String message) {
            this.timestamp = timestamp;
            this.context = context;
            this.sqlState = sqlState;
            this.message = message;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public String getContext() {
            return context;
        }

        public String getSqlState() {
            return sqlState;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Borrows a pooled JDBC {@link Connection} wrapped so {@code close()} returns it to the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (SHUTTING_DOWN.get()) {
            throw new SQLException("Database connection pool is shutting down.");
        }

        Connection physical;
        while ((physical = POOL.pollFirst()) != null) {
            if (isConnectionHealthy(physical)) {
                return wrapConnection(physical);
            }
            closePhysicalQuietly(physical);
        }

        physical = openPhysicalConnection();
        ALL_PHYSICAL_CONNECTIONS.add(physical);
        return wrapConnection(physical);
    }

    private static Connection openPhysicalConnection() throws SQLException {
        loadDerbyClientDriver();
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException ex) {
            String state = ex.getSQLState();
            // XJ004 = database does not exist yet on server
            if ("XJ004".equalsIgnoreCase(state)) {
                return DriverManager.getConnection(DB_URL + ";create=true", DB_USER, DB_PASSWORD);
            }
            // 08xxx = network connection error (Derby Network Server not started yet)
            if (state != null && state.startsWith("08")) {
                try {
                    ensureNetworkServerStarted();
                    try {
                        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    } catch (SQLException secondEx) {
                        if ("XJ004".equalsIgnoreCase(secondEx.getSQLState())) {
                            return DriverManager.getConnection(DB_URL + ";create=true", DB_USER, DB_PASSWORD);
                        }
                        throw secondEx;
                    }
                } catch (SQLException serverEx) {
                    recordError("Connection Attempt", serverEx);
                    throw new SQLException(
                            "Unable to connect to Apache Derby database '" + DATABASE_NAME + "' at "
                            + HOST + ":" + PORT + ". Please ensure Java DB / Apache Derby is accessible. Details: "
                            + serverEx.getMessage(),
                            serverEx.getSQLState(),
                            serverEx
                    );
                }
            }
            recordError("Connection Attempt", ex);
            throw new SQLException(
                    "Database connection failed (" + (state == null ? "UNKNOWN" : state) + "): " + ex.getMessage(),
                    state,
                    ex
            );
        }
    }

    private static void loadDerbyClientDriver() throws SQLException {
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException first) {
            try {
                Class.forName("org.apache.derby.client.ClientAutoloadedDriver");
            } catch (ClassNotFoundException second) {
                SQLException err = new SQLException(
                        "Apache Derby JDBC Driver (derbyclient.jar) was not found on the classpath.",
                        "08001",
                        second
                );
                recordError("Driver Loading", err);
                throw err;
            }
        }
    }

    /**
     * Starts an in-process Apache Derby Network Server if port 1527 is not already active,
     * pointing {@code derby.system.home} to the existing NetBeansProjects folder when present.
     */
    private static synchronized void ensureNetworkServerStarted() throws SQLException {
        if (managedServerControl != null) {
            return;
        }
        try {
            if (System.getProperty("derby.system.home") == null) {
                File parentAdminDb = new File("..", DATABASE_NAME);
                File localAdminDb = new File(".", DATABASE_NAME);
                if (parentAdminDb.isDirectory()) {
                    System.setProperty("derby.system.home", new File("..").getCanonicalPath());
                } else if (localAdminDb.isDirectory()) {
                    System.setProperty("derby.system.home", new File(".").getCanonicalPath());
                } else {
                    File netbeansProjects = new File("..").getCanonicalFile();
                    System.setProperty("derby.system.home", netbeansProjects.getAbsolutePath());
                }
            }

            Class<?> controlClass = Class.forName("org.apache.derby.drda.NetworkServerControl");
            Object control = controlClass
                    .getConstructor(InetAddress.class, int.class)
                    .newInstance(InetAddress.getByName(HOST), PORT);
            Method startMethod = controlClass.getMethod("start", PrintWriter.class);
            startMethod.invoke(control, new Object[]{null});

            Method pingMethod = controlClass.getMethod("ping");
            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                try {
                    pingMethod.invoke(control);
                    managedServerControl = control;
                    embeddedServerStartedByApp = true;
                    return;
                } catch (Exception ignored) {
                    Thread.sleep(150);
                }
            }
            throw new SQLException("Timed out waiting for Apache Derby Network Server to accept connections.", "08001");
        } catch (SQLException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SQLException("Could not start Apache Derby Network Server automatically: " + ex.getMessage(), "08001", ex);
        }
    }

    private static boolean isConnectionHealthy(Connection con) {
        if (con == null) {
            return false;
        }
        try {
            return !con.isClosed() && con.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    private static Connection wrapConnection(Connection physical) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean returned = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("close".equals(name)) {
                    if (!returned) {
                        returned = true;
                        releaseConnection(physical);
                    }
                    return null;
                }
                if ("isClosed".equals(name)) {
                    return returned || physical.isClosed();
                }
                try {
                    return method.invoke(physical, args);
                } catch (InvocationTargetException ex) {
                    Throwable target = ex.getTargetException();
                    if (target instanceof SQLException) {
                        recordError("SQL Execution", target);
                    }
                    throw target;
                }
            }
        };

        return (Connection) Proxy.newProxyInstance(
                DatabaseConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private static void releaseConnection(Connection physical) {
        if (physical == null) {
            return;
        }
        if (SHUTTING_DOWN.get() || POOL.size() >= MAX_POOL_SIZE) {
            closePhysicalQuietly(physical);
            return;
        }
        try {
            if (physical.isClosed()) {
                ALL_PHYSICAL_CONNECTIONS.remove(physical);
                return;
            }
            if (!physical.getAutoCommit()) {
                try {
                    physical.rollback();
                } catch (SQLException ignored) {
                }
                physical.setAutoCommit(true);
            }
            POOL.offerFirst(physical);
        } catch (SQLException ex) {
            closePhysicalQuietly(physical);
        }
    }

    private static void closePhysicalQuietly(Connection physical) {
        if (physical == null) {
            return;
        }
        ALL_PHYSICAL_CONNECTIONS.remove(physical);
        try {
            if (!physical.isClosed()) {
                physical.close();
            }
        } catch (SQLException ignored) {
        }
    }

    /**
     * Records a sanitized database error message for monitoring in the Admin Database console.
     */
    public static void recordError(String context, Throwable error) {
        if (error == null) {
            return;
        }
        String sqlState = (error instanceof SQLException) ? ((SQLException) error).getSQLState() : "-";
        String rawMsg = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        String sanitized = rawMsg.replace(DB_PASSWORD, "***");
        synchronized (ERROR_HISTORY) {
            if (ERROR_HISTORY.size() >= MAX_ERROR_LOG_SIZE) {
                ERROR_HISTORY.remove(0);
            }
            ERROR_HISTORY.add(new DatabaseErrorRecord(
                    new Timestamp(System.currentTimeMillis()),
                    context,
                    sqlState == null ? "-" : sqlState,
                    sanitized
            ));
        }
    }

    public static List<DatabaseErrorRecord> getRecentErrors() {
        synchronized (ERROR_HISTORY) {
            return new ArrayList<>(ERROR_HISTORY);
        }
    }

    public static void clearRecordedErrors() {
        synchronized (ERROR_HISTORY) {
            ERROR_HISTORY.clear();
        }
    }

    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return con != null && !con.isClosed() && con.isValid(2);
        } catch (SQLException ex) {
            recordError("Health Check", ex);
            return false;
        }
    }

    public static int getPooledConnectionCount() {
        return POOL.size();
    }

    public static int getTotalCreatedConnectionCount() {
        return ALL_PHYSICAL_CONNECTIONS.size();
    }

    public static String getDatabaseUser() {
        return DB_USER;
    }

    public static boolean isEmbeddedServerStartedByApp() {
        return embeddedServerStartedByApp;
    }

    /**
     * Closes all pooled connections and shuts down an app-started Derby server if applicable.
     */
    public static synchronized void shutdown() {
        SHUTTING_DOWN.set(true);
        Connection c;
        while ((c = POOL.pollFirst()) != null) {
            closePhysicalQuietly(c);
        }
        List<Connection> remaining;
        synchronized (ALL_PHYSICAL_CONNECTIONS) {
            remaining = new ArrayList<>(ALL_PHYSICAL_CONNECTIONS);
        }
        for (Connection conn : remaining) {
            closePhysicalQuietly(conn);
        }
        if (embeddedServerStartedByApp && managedServerControl != null) {
            try {
                Method shutdownMethod = managedServerControl.getClass().getMethod("shutdown");
                shutdownMethod.invoke(managedServerControl);
            } catch (Exception ignored) {
            } finally {
                managedServerControl = null;
                embeddedServerStartedByApp = false;
            }
        }
    }
}
