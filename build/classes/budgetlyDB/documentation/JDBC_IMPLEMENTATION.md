# JDBC Implementation Guide

This document explains how **JDBC (Java Database Connectivity)** is implemented across `budgetlyDB` to communicate reliably and securely with Apache Derby.

---

## 1. Centralized Connection Manager (`DatabaseConnection.java`)

### Why We Use a Connection Pool Wrapper
Opening a fresh TCP socket connection to Apache Derby on every button click is wasteful and slow. At the same time, sharing a single raw `Connection` object blindly can break if a caller closes it inside a `try-with-resources` block or if two threads toggle `setAutoCommit(false)` concurrently.

`DatabaseConnection` solves both problems cleanly:

- Maintains a bounded pool (`ConcurrentLinkedDeque<Connection>`) of up to `MAX_POOL_SIZE = 4` healthy physical connections to `jdbc:derby://localhost:1527/AdminDB` as `adminUser`.
- Wraps each borrowed physical connection in a Java dynamic `Proxy` (`wrapConnection(Connection physical)`).
- When a DAO finishes its `try (Connection con = DatabaseConnection.getConnection())` block, `con.close()` intercepts the call, rolls back any uncommitted state if `autoCommit` was `false`, resets `autoCommit = true`, and returns the physical connection to the pool for reuse.
- If the Derby Network Server on port `1527` is not yet running (`SQLState` starting with `08`), `DatabaseConnection.ensureNetworkServerStarted()` boots `org.apache.derby.drda.NetworkServerControl` automatically so the app remains self-healing.

```java
// Standard DAO usage with automatic pooling and clean resource closing:
try (Connection con = DatabaseConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setInt(1, userId);
    try (ResultSet rs = ps.executeQuery()) {
        // map ResultSet rows...
    }
}
```

---

## 2. Schema Initialization (`DatabaseInitializer.java`)

Unlike MySQL or PostgreSQL, Apache Derby does **not** support `CREATE TABLE IF NOT EXISTS`. Instead, `DatabaseInitializer` inspects JDBC metadata before running `CREATE TABLE` DDL statements:

```java
private static boolean tableExists(DatabaseMetaData meta, String tableName) throws SQLException {
    try (ResultSet rs = meta.getTables(null, DatabaseConnection.SCHEMA_NAME,
            tableName.toUpperCase(), new String[]{"TABLE"})) {
        return rs.next();
    }
}
```

- `Statement` is used strictly for static, parameter-free `CREATE TABLE` DDL commands during initialization.
- Every data query (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) uses `PreparedStatement` with `?` placeholders.
- Existing tables are skipped, guaranteeing existing records survive restarts.

---

## 3. CRUD Operations with `PreparedStatement` & `ResultSet`

### Why `PreparedStatement` Instead of `Statement`?
1. **Prevents SQL Injection**: Parameters bound via `ps.setString(1, value)` are treated purely as data values, never as executable SQL syntax.
2. **Type Safety**: `setInt`, `setBigDecimal`, `setDate`, and `setBoolean` preserve exact Java and SQL types.
3. **Query Plan Caching**: Derby can reuse compiled execution plans for repeated queries.

### Examples from Actual Budgetly DAOs

- **INSERT with Generated Key (`UserDAO.insertUser`)**:
```java
String sql = "INSERT INTO USERS "
        + "(username, password_hash, password_salt, email, first_name, last_name, account_type, account_status) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
    ps.setString(1, user.getUsername());
    // ... bind remaining columns ...
    ps.executeUpdate();
    try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
            return keys.getInt(1);
        }
    }
}
```

- **SELECT (`TransactionDAO.getMonthlyTotal`)**:
```java
String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM TRANSACTIONS "
        + "WHERE user_id = ? AND transaction_type = ? "
        + "AND transaction_date >= ? AND transaction_date < ?";
try (Connection con = DatabaseConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setInt(1, userId);
    ps.setString(2, type.toUpperCase());
    ps.setDate(3, Date.valueOf(start));
    ps.setDate(4, Date.valueOf(end));
    try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
    }
}
```

- **UPDATE (`FinancialAccountDAO.adjustBalance`)**:
```java
String sql = "UPDATE FINANCIAL_ACCOUNTS SET balance = balance + ? WHERE account_id = ? AND user_id = ?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setBigDecimal(1, delta);
    ps.setInt(2, accountId);
    ps.setInt(3, userId);
    return ps.executeUpdate() > 0;
}
```

- **DELETE (`TransactionDAO.deleteByIdForUser`)**:
```java
String sql = "DELETE FROM TRANSACTIONS WHERE transaction_id = ? AND user_id = ?";
try (PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setInt(1, transactionId);
    ps.setInt(2, userId);
    return ps.executeUpdate() > 0;
}
```

---

## 4. Database Transactions (Atomicity)

Multi-step operations must either **all succeed** or **all fail together** so the database is never left half-updated.

Budgetly uses explicit JDBC transactions (`con.setAutoCommit(false)`, `con.commit()`, `con.rollback()`) in:
1. **`DatabaseInitializer.ensureDefaultAdmin`**: Inserts `USERS`, `USER_PROFILES`, `USER_SETTINGS`, starter `FINANCIAL_ACCOUNTS`, and `USER_ACTIVITY_LOGS` atomically.
2. **`AuthenticationService.registerUser`**: Creates the user account, profile, settings, starter `"Cash Wallet"`, and signup log atomically.
3. **`BudgetService.recordTransaction`**: Verifies account ownership, inserts into `TRANSACTIONS`, adjusts `FINANCIAL_ACCOUNTS.balance`, and logs activity atomically.
4. **`BudgetService.deleteTransaction`**: Reads the transaction, reverses its balance impact on `FINANCIAL_ACCOUNTS`, deletes the transaction, and logs the deletion atomically.

---

## 5. Exception Handling & Error Logging

- Every JDBC call catches or propagates `SQLException` cleanly.
- `DatabaseConnection.recordError(context, error)` sanitizes error messages (stripping credentials) and records timestamp, context, `SQLState`, and message in memory so administrators can inspect database issues inside `DatabaseManagementFrame`.
- User-facing dialogs display helpful explanations rather than raw stack traces.
