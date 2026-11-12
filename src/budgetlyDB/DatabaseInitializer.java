package budgetlyDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initializes all required Apache Derby tables and ensures the default administrator account
 * exists without duplicating existing schema objects or overwriting preserved user records.
 */
public final class DatabaseInitializer {

    private static volatile boolean initialized = false;
    private static volatile String lastInitializationMessage = "Not initialized yet.";

    private DatabaseInitializer() {
    }

    /**
     * Initializes schema tables and seeds the default administrator account if missing.
     * Safe to call multiple times; runs once per application session unless reset.
     */
    public static synchronized void initialize() throws SQLException {
        if (initialized) {
            return;
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            int tablesCreated = 0;

            if (!tableExists(meta, "USERS")) {
                executeDDL(con,
                        "CREATE TABLE USERS ("
                        + "user_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "username VARCHAR(50) NOT NULL UNIQUE, "
                        + "password_hash VARCHAR(256) NOT NULL, "
                        + "password_salt VARCHAR(128) NOT NULL, "
                        + "email VARCHAR(120) NOT NULL UNIQUE, "
                        + "first_name VARCHAR(60) NOT NULL, "
                        + "last_name VARCHAR(60) NOT NULL, "
                        + "account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ADMIN', 'USER')), "
                        + "account_status VARCHAR(20) NOT NULL CHECK (account_status IN ('ACTIVE', 'INACTIVE')), "
                        + "date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "last_login TIMESTAMP"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "USER_PROFILES")) {
                executeDDL(con,
                        "CREATE TABLE USER_PROFILES ("
                        + "profile_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT NOT NULL UNIQUE REFERENCES USERS(user_id) ON DELETE CASCADE, "
                        + "profile_picture VARCHAR(255) DEFAULT 'default_croc', "
                        + "display_name VARCHAR(100) NOT NULL, "
                        + "contact_number VARCHAR(30) DEFAULT '', "
                        + "address VARCHAR(255) DEFAULT '', "
                        + "bio VARCHAR(500) DEFAULT '', "
                        + "occupation VARCHAR(100) DEFAULT '', "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "USER_SETTINGS")) {
                executeDDL(con,
                        "CREATE TABLE USER_SETTINGS ("
                        + "setting_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT NOT NULL UNIQUE REFERENCES USERS(user_id) ON DELETE CASCADE, "
                        + "theme VARCHAR(30) NOT NULL DEFAULT 'LIGHT', "
                        + "notification_preferences VARCHAR(100) NOT NULL DEFAULT 'BUDGET_AND_SECURITY', "
                        + "budget_alerts_enabled BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "email_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "currency_preference VARCHAR(10) NOT NULL DEFAULT 'PHP', "
                        + "default_budget_alert_percent INT NOT NULL DEFAULT 80, "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "USER_ACTIVITY_LOGS")) {
                executeDDL(con,
                        "CREATE TABLE USER_ACTIVITY_LOGS ("
                        + "log_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT REFERENCES USERS(user_id) ON DELETE SET NULL, "
                        + "username_snapshot VARCHAR(50), "
                        + "activity_type VARCHAR(40) NOT NULL, "
                        + "activity_description VARCHAR(500) NOT NULL, "
                        + "activity_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "FINANCIAL_ACCOUNTS")) {
                executeDDL(con,
                        "CREATE TABLE FINANCIAL_ACCOUNTS ("
                        + "account_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE, "
                        + "account_name VARCHAR(80) NOT NULL, "
                        + "account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('CASH', 'BANK', 'EWALLET', 'OTHER')), "
                        + "balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00, "
                        + "notes VARCHAR(255) DEFAULT '', "
                        + "date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT uq_user_account_name UNIQUE (user_id, account_name)"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "TRANSACTIONS")) {
                executeDDL(con,
                        "CREATE TABLE TRANSACTIONS ("
                        + "transaction_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE, "
                        + "account_id INT NOT NULL REFERENCES FINANCIAL_ACCOUNTS(account_id) ON DELETE CASCADE, "
                        + "transaction_type VARCHAR(15) NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE')), "
                        + "category VARCHAR(60) NOT NULL, "
                        + "amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0), "
                        + "description VARCHAR(255) DEFAULT '', "
                        + "transaction_date DATE NOT NULL, "
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ")");
                tablesCreated++;
            }

            if (!tableExists(meta, "MONTHLY_BUDGETS")) {
                executeDDL(con,
                        "CREATE TABLE MONTHLY_BUDGETS ("
                        + "budget_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, "
                        + "user_id INT NOT NULL REFERENCES USERS(user_id) ON DELETE CASCADE, "
                        + "budget_year INT NOT NULL, "
                        + "budget_month INT NOT NULL CHECK (budget_month BETWEEN 1 AND 12), "
                        + "category VARCHAR(60) NOT NULL DEFAULT 'OVERALL', "
                        + "limit_amount DECIMAL(15, 2) NOT NULL CHECK (limit_amount > 0), "
                        + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT uq_user_month_cat UNIQUE (user_id, budget_year, budget_month, category)"
                        + ")");
                tablesCreated++;
            }

            boolean adminCreated = ensureDefaultAdmin(con);
            initialized = true;
            lastInitializationMessage = "Database ready (" + tablesCreated + " table(s) created, "
                    + (adminCreated ? "default admin seeded" : "existing data preserved") + ").";
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Database Initialization", ex);
            lastInitializationMessage = "Initialization failed: " + ex.getMessage();
            throw ex;
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String tableName) throws SQLException {
        try (ResultSet rs = meta.getTables(null, DatabaseConnection.SCHEMA_NAME, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static void executeDDL(Connection con, String ddl) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(ddl);
        }
    }

    /**
     * Ensures a default administrator account ('admin' / 'admin') exists in USERS.
     * Uses PBKDF2WithHmacSHA256 hashing and creates profile, settings, and starter account
     * inside one atomic database transaction.
     */
    private static boolean ensureDefaultAdmin(Connection con) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM USERS WHERE LOWER(username) = 'admin' OR account_type = 'ADMIN'";
        try (PreparedStatement checkPs = con.prepareStatement(checkSql);
             ResultSet rs = checkPs.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return false;
            }
        }

        boolean previousAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            PasswordHasher.HashedPassword hashed = PasswordHasher.hashPassword(new char[]{'a', 'd', 'm', 'i', 'n'});

            String insertUserSql = "INSERT INTO USERS "
                    + "(username, password_hash, password_salt, email, first_name, last_name, account_type, account_status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, 'ADMIN', 'ACTIVE')";
            int adminUserId;
            try (PreparedStatement ps = con.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "admin");
                ps.setString(2, hashed.getHash());
                ps.setString(3, hashed.getSalt());
                ps.setString(4, "admin@budgetly.local");
                ps.setString(5, "System");
                ps.setString(6, "Administrator");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to retrieve generated user_id for default admin.");
                    }
                    adminUserId = keys.getInt(1);
                }
            }

            String insertProfileSql = "INSERT INTO USER_PROFILES "
                    + "(user_id, profile_picture, display_name, contact_number, address, bio, occupation) "
                    + "VALUES (?, 'default_croc', ?, '', '', 'Default Budgetly System Administrator', 'System Administrator')";
            try (PreparedStatement ps = con.prepareStatement(insertProfileSql)) {
                ps.setInt(1, adminUserId);
                ps.setString(2, "System Administrator");
                ps.executeUpdate();
            }

            String insertSettingsSql = "INSERT INTO USER_SETTINGS "
                    + "(user_id, theme, notification_preferences, budget_alerts_enabled, email_summary_enabled, currency_preference, default_budget_alert_percent) "
                    + "VALUES (?, 'LIGHT', 'BUDGET_AND_SECURITY', TRUE, FALSE, 'PHP', 80)";
            try (PreparedStatement ps = con.prepareStatement(insertSettingsSql)) {
                ps.setInt(1, adminUserId);
                ps.executeUpdate();
            }

            String insertAccountSql = "INSERT INTO FINANCIAL_ACCOUNTS "
                    + "(user_id, account_name, account_type, balance, notes) "
                    + "VALUES (?, 'Cash Wallet', 'CASH', 0.00, 'Default starter wallet')";
            try (PreparedStatement ps = con.prepareStatement(insertAccountSql)) {
                ps.setInt(1, adminUserId);
                ps.executeUpdate();
            }

            String insertLogSql = "INSERT INTO USER_ACTIVITY_LOGS "
                    + "(user_id, username_snapshot, activity_type, activity_description) "
                    + "VALUES (?, 'admin', 'DB_INIT', 'Default administrator account seeded automatically during initialization.')";
            try (PreparedStatement ps = con.prepareStatement(insertLogSql)) {
                ps.setInt(1, adminUserId);
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException ex) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }
            throw ex;
        } finally {
            con.setAutoCommit(previousAutoCommit);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getLastInitializationMessage() {
        return lastInitializationMessage;
    }
}
