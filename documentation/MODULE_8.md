# MODULE 8 – Database Programming with JDBC and Java Application Integration

## 1. Module Title
**Module 8: Database Programming with JDBC and Java Application Integration**

## 2. Concepts Covered
- **Apache Derby (Java DB) & Derby Client Driver**: Connecting to `jdbc:derby://localhost:1527/BudgetlyDB;create=true` using `derbyclient.jar`.
- **Relational Schema Design**: `USERS`, `TRANSACTIONS`, and `BUDGETS` tables with `PRIMARY KEY`, `FOREIGN KEY (user_id)`, `UNIQUE (username)`, and `CHECK` constraints (`database/schema.sql`).
- **Centralized JDBC Connection (`DatabaseConnection.java`)**: Opening connections with `DriverManager.getConnection(...)` and initializing tables on first run.
- **`PreparedStatement` and SQL Injection Prevention**: Using parameterized queries (`?` placeholders) for all `INSERT`, `SELECT`, `UPDATE`, and `DELETE` operations.
- **`ResultSet` Mapping**: Reading rows returned by `SELECT` queries and converting them into `Admin`, `RegularUser`, `Income`, `Expense`, and `Budget` objects.
- **User Isolation (`WHERE user_id = ?`)**: Enforcing the logged-in user's ID in every query so users can only view or modify their own data.

---

## 3. Complete Application-to-Database Flow in BUDGETLY
```
Swing GUI (MainFrame / LoginFrame / AdminDashboard)
   │  User clicks button -> ActionListener reads form fields
   ▼
Service Layer (AuthService / BudgetService)
   │  Validates input, hashes password or parses BigDecimal/LocalDate
   ▼
DAO Layer (UserDAO / TransactionDAO / BudgetDAO)
   │  Creates PreparedStatement with ? parameters and user_id check
   ▼
DatabaseConnection.getConnection()
   │  Opens JDBC connection via Derby Client Driver
   ▼
Apache Derby Database (localhost:1527/BudgetlyDB)
   │  Executes SQL and returns ResultSet / affected row count
   ▼
DAO maps ResultSet rows -> Model objects -> Service -> Swing JTable / JLabel
```

---

## 4. Actual Source Code from BUDGETLY

### Example A – Centralized Derby Client Connection (`DatabaseConnection.java`)
- **Source File**: `src/budgetly/database/DatabaseConnection.java`
- **Class & Method**: `DatabaseConnection.getConnection()`

```java
public static final String CLIENT_DRIVER = "org.apache.derby.client.ClientAutoloadedDriver";
public static final String URL = "jdbc:derby://localhost:1527/BudgetlyDB;create=true";
private static final String DB_USER = "budgetly";
private static final String DB_PASSWORD = "budgetly";
private static final int PORT = 1527;

public static Connection getConnection() throws SQLException {
    startNetworkServerIfNeeded();
    return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
}
```

### Example B – User Registration & Authentication Queries (`UserDAO.java`)
- **Source File**: `src/budgetly/dao/UserDAO.java`
- **Class & Methods**: `UserDAO.insertUser(...)` and `UserDAO.findByUsername(...)`

```java
public void insertUser(String fullName, String username, String passwordHash,
                       UserRole role) throws SQLException {
    String sql = "INSERT INTO users (full_name, username, password_hash, role) VALUES (?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, fullName);
        ps.setString(2, username);
        ps.setString(3, passwordHash);
        ps.setString(4, role.name());
        ps.executeUpdate();
    }
}

public User findByUsername(String username) throws SQLException {
    String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE LOWER(username) = LOWER(?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapRow(rs) : null;
        }
    }
}
```

### Example C – Full CRUD Operations Scoped by `user_id` (`TransactionDAO.java`)
- **Source File**: `src/budgetly/dao/TransactionDAO.java`
- **Class & Methods**: `TransactionDAO.insert(...)`, `TransactionDAO.update(...)`, and `TransactionDAO.delete(...)`

```java
public void insert(Transaction t) throws SQLException {
    String sql = "INSERT INTO transactions (user_id, description, amount, category, "
            + "transaction_type, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, t.getUserId());
        ps.setString(2, t.getDescription());
        ps.setBigDecimal(3, t.getAmount());
        ps.setString(4, t.getCategory());
        ps.setString(5, t.getType().name());
        ps.setDate(6, Date.valueOf(t.getDate()));
        ps.executeUpdate();
    }
}

public boolean update(Transaction t) throws SQLException {
    String sql = "UPDATE transactions SET description = ?, amount = ?, category = ?, "
            + "transaction_date = ? WHERE transaction_id = ? AND user_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, t.getDescription());
        ps.setBigDecimal(2, t.getAmount());
        ps.setString(3, t.getCategory());
        ps.setDate(4, Date.valueOf(t.getDate()));
        ps.setInt(5, t.getTransactionId());
        ps.setInt(6, t.getUserId());
        return ps.executeUpdate() == 1;
    }
}

public boolean delete(int transactionId, int userId) throws SQLException {
    String sql = "DELETE FROM transactions WHERE transaction_id = ? AND user_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, transactionId);
        ps.setInt(2, userId);
        return ps.executeUpdate() == 1;
    }
}
```

---

## 5. Explanation of Important Lines of Code
- `DriverManager.getConnection(URL, DB_USER, DB_PASSWORD)`: Opens a client-server JDBC connection to Apache Derby on `localhost:1527`.
- `PreparedStatement ps = conn.prepareStatement(sql)`: Pre-compiles the SQL statement and binds parameters safely (`ps.setInt`, `ps.setString`, `ps.setBigDecimal`, `ps.setDate`), preventing SQL injection.
- `WHERE transaction_id = ? AND user_id = ?`: Ensures a user can never update or delete another user's transaction even if a different transaction ID is passed.
- `try (ResultSet rs = ps.executeQuery())`: Executes a `SELECT` query and closes the `ResultSet` automatically when finished.

## 6. How It Works Within the Application
When a user adds, edits, or deletes an income, expense, or budget in `MainFrame`, the event listener calls `BudgetService`, which validates the input and calls `TransactionDAO` or `BudgetDAO`. The DAO executes a `PreparedStatement` against Apache Derby. Next, `MainFrame.refreshAllData()` runs `TransactionDAO.findByUser(userId)` inside a `SwingWorker`, maps the `ResultSet` rows into `Income` and `Expense` objects, and populates the Swing `JTable` and Dashboard summary cards.
