# Budgetly Database, Authentication & Management System

Welcome to the technical documentation for **Budgetly**, a Java Swing desktop personal finance and budget tracking application backed by **Apache Derby (Java DB)** via **JDBC**.

---

## 1. What the Budgetly Database System Does

Budgetly allows multiple users to register personal accounts, sign in securely, and manage their own isolated financial data while preserving every record permanently in an Apache Derby relational database (`AdminDB`).

Key capabilities implemented in `budgetlyDB` and integrated with `budgetly`:

- **Automatic Database Initialization**: Creates all 7 relational tables in schema `ADMINUSER` on startup if they do not exist, while preserving all existing user records across restarts.
- **Secure User & Admin Authentication**: Supports user signup, login, password hashing with `PBKDF2WithHmacSHA256`, unique per-password salts, constant-time verification, and session management.
- **Per-User Financial Isolation**: Each user's cash/bank/e-wallet accounts, income, expenses, monthly budgets, profiles, and settings are linked to their `user_id` via foreign keys and enforced in the DAO and service layers.
- **Administrator Dashboard & Monitoring**: Provides KPI cards, user search, account activation/deactivation, user editing/deletion, activity logs, and safe read-only Derby table inspection.
- **User Profile & Preferences**: Persists display name, contact details, bio, preferred currency (`PHP`, `USD`, `EUR`, `GBP`, `JPY`), UI theme (`LIGHT`, `FOREST`, `DARK`), and budget alert settings.
- **Java Modules Documentation**: External `docs/JAVA_MODULES.md` guide covering Modules 1 through 8 with actual Budgetly source excerpts.

---

## 2. Technologies Used

| Component | Technology / Version | Purpose |
| :--- | :--- | :--- |
| Language & Runtime | Java SE 21 (JDK 21) | Core application logic and Swing desktop UI |
| Database Engine | Apache Derby (Java DB) 10.17.1.0 | Relational database (`AdminDB`) running on `localhost:1527` |
| Database API | JDBC (`derbyclient.jar`, `derbyshared.jar`, `derby.jar`, `derbynet.jar`) | Parameterized SQL, transactions, metadata inspection |
| Graphical Interface | Java Swing (`JFrame`, `JPanel`, `JTable`, `SwingWorker`) | Desktop windows matching Budgetly's forest-green theme |
| Password Security | `SecretKeyFactory` (`PBKDF2WithHmacSHA256`) + `SecureRandom` | Salted key derivation and constant-time hash comparison |

---

## 3. Package Structure

```text
src/
├── budgetly/
│   ├── Budgetly.java                 # Main entry point (initializes DB, opens LoginFrame)
│   ├── resources/
│   │   ├── budgetly-logo.png         # Full Budgetly mascot logo
│   │   └── budgetly-icon.png         # Cropped mascot window/taskbar icon
│   └── ui/
│       ├── MainFrame.java            # Main Budgetly application JFrame (CardLayout navigation)
│       ├── DashboardPanel.java       # Live monthly summary, budget alert, recent transactions
│       ├── AccountsPanel.java        # Cash, Bank, E-Wallet, Other account management
│       ├── TransactionEntryPanel.java# Shared Income/Expense form & recent table
│       ├── IncomePanel.java          # Income recording screen
│       ├── ExpensesPanel.java        # Expense recording screen
│       ├── MonthlyBudgetPanel.java   # Overall & category monthly limits
│       ├── TransactionHistoryPanel.java # Searchable & filterable transaction table
│       └── ProfilePanel.java         # Embedded user profile, settings, and password change
│
└── budgetlyDB/
    ├── DatabaseConnection.java       # Pooled JDBC connection manager for Apache Derby
    ├── DatabaseInitializer.java      # Idempotent schema creation and default admin seeding
    ├── PasswordHasher.java           # PBKDF2WithHmacSHA256 + SecureRandom salt hashing
    ├── SessionManager.java           # Active user session and role authorization checks
    ├── AuthenticationService.java    # Login, transactional signup, password change, logout
    ├── DatabaseManager.java          # Derby metadata diagnostics and error log monitor
    ├── BudgetService.java            # Atomic financial transactions and user isolation
    ├── UserDAO.java                  # USERS table CRUD and search queries
    ├── AdminDAO.java                 # Admin metrics, user administration, safe table preview
    ├── ProfileDAO.java               # USER_PROFILES CRUD
    ├── SettingsDAO.java              # USER_SETTINGS CRUD
    ├── ActivityLogDAO.java           # USER_ACTIVITY_LOGS audit trail
    ├── FinancialAccountDAO.java      # FINANCIAL_ACCOUNTS CRUD
    ├── TransactionDAO.java           # TRANSACTIONS CRUD & monthly aggregations
    ├── MonthlyBudgetDAO.java         # MONTHLY_BUDGETS CRUD & spending comparisons
    ├── model/                        # User, Admin, UserProfile, UserSettings, FinancialAccount, etc.
    ├── ui/                           # LoginFrame, SignupFrame, AdminDashboard, UserManagementFrame, etc.
    └── documentation/                # Project technical documentation in Markdown
```

---

## 4. How the Database Is Initialized & How to Run

1. When `budgetly.Budgetly.main()` starts, it immediately calls `DatabaseInitializer.initialize()` before displaying any login or signup window.
2. `DatabaseConnection` connects to Apache Derby Network Server at `jdbc:derby://localhost:1527/AdminDB` using the configured database credentials (`adminUser` / `admin`). If port `1527` is not yet listening, `DatabaseConnection` automatically starts an in-process Derby `NetworkServerControl` pointing to `AdminDB`.
3. `DatabaseInitializer` inspects `DatabaseMetaData.getTables(...)` for schema `ADMINUSER` and creates any missing tables (`USERS`, `USER_PROFILES`, `USER_SETTINGS`, `USER_ACTIVITY_LOGS`, `FINANCIAL_ACCOUNTS`, `TRANSACTIONS`, `MONTHLY_BUDGETS`). Existing tables and records are preserved.
4. If no administrator exists yet, a default administrator account is seeded once:
   - **Username:** `admin`
   - **Password:** `admin` *(hashed with PBKDF2WithHmacSHA256 and a random salt; change it inside Admin Dashboard -> Admin Settings)*
5. Regular users can click **Create New Account** on `LoginFrame` to register via `SignupFrame`, then sign in to open `MainFrame`.

---

## 5. Documentation Index

- [DATABASE_ARCHITECTURE.md](DATABASE_ARCHITECTURE.md) — Layered architecture, JDBC flow, and user data isolation
- [JDBC_IMPLEMENTATION.md](JDBC_IMPLEMENTATION.md) — Connection pooling, `PreparedStatement`, transactions, and error handling
- [AUTHENTICATION_SYSTEM.md](AUTHENTICATION_SYSTEM.md) — Login, registration, session management, and role checks
- [PASSWORD_HASHING.md](PASSWORD_HASHING.md) — Beginner-friendly deep dive into `PBKDF2WithHmacSHA256` and salts
- [ADMIN_SYSTEM.md](ADMIN_SYSTEM.md) — Admin Dashboard, User Management, Database Monitor, and authorization
- [USER_PROFILE_AND_SETTINGS.md](USER_PROFILE_AND_SETTINGS.md) — Profile editing, currency/theme settings, and persistence
- [SWING_GUI_IMPLEMENTATION.md](SWING_GUI_IMPLEMENTATION.md) — Swing `JFrame` architecture, `BrandTheme`, and `SwingWorker`
- [JAVA_MODULES.md](JAVA_MODULES.md) — Modules 1–8 programming guide using actual coursework and Budgetly code
- [DATABASE_TABLES.md](DATABASE_TABLES.md) — Complete column specifications and Mermaid ERD
- [TESTING_AND_SETUP.md](TESTING_AND_SETUP.md) — Setup instructions, build commands, verification checklist, and troubleshooting
