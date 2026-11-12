# Testing, Setup & Troubleshooting Guide

---

## 1. Environment & Prerequisites

- **Java Version**: JDK 21 (configured in `nbproject/project.properties`: `javac.source=21`, `javac.target=21`)
- **Database Engine**: Apache Derby / Java DB `10.17.1.0`
- **JDBC Endpoint**: `jdbc:derby://localhost:1527/AdminDB`
- **Schema**: `ADMINUSER`
- **Configured Database Credentials**: Username `adminUser`, Password `admin`
- **Required Derby Libraries** (configured in `nbproject/project.properties` via `JAVADB_DRIVER_LABEL` + `derbyshared.jar`):
  - `derby.jar`
  - `derbyclient.jar`
  - `derbynet.jar`
  - `derbyshared.jar`

---

## 2. How to Launch Budgetly

### Option A: Inside Apache NetBeans 25
1. Open the **Budgetly** project in NetBeans.
2. *(Optional)* In the **Services** tab -> **Databases** -> right-click **Java DB** -> **Start Server**. *(If you skip this step, `DatabaseConnection` automatically starts the Derby Network Server on port `1527`.)*
3. Click **Run Project (`F6`)** to launch `budgetly.Budgetly`.

### Option B: Command Line / Ant Build
```powershell
# Clean, compile, and package dist/Budgetly.jar with dist/lib/*
& "C:\Program Files\NetBeans-25\netbeans\extide\ant\bin\ant.bat" clean jar

# Run the packaged application
java -jar dist\Budgetly.jar
```

---

## 3. Initial Login & Verification Walkthrough

1. **Database Initialization**: On launch, `DatabaseInitializer.initialize()` verifies or creates the 7 tables (`USERS`, `USER_PROFILES`, `USER_SETTINGS`, `USER_ACTIVITY_LOGS`, `FINANCIAL_ACCOUNTS`, `TRANSACTIONS`, `MONTHLY_BUDGETS`) and seeds the default admin account once.
2. **Default Administrator Login**:
   - Open `LoginFrame`
   - Username: `admin`
   - Password: `admin`
   - Click **Sign In** -> `MainFrame` opens with an **Admin Dashboard** button in the sidebar and banner.
3. **Regular User Registration & Isolation Test**:
   - Click **Logout** -> **Create New Account** (`SignupFrame`).
   - Register User A (e.g., `alice`), log in, create a Bank account, record Income and Expense, set a Monthly Budget, and change Currency/Theme in **Profile**.
   - Log out and register User B (e.g., `bob`). Verify User B sees only their own starter Cash Wallet and `0.00` transactions — never User A's records.
   - Log out and sign back in as User A. Verify User A's balances, transactions, budgets, profile, and theme/currency preferences are restored from Derby.
4. **Java Modules Documentation Test**:
   - Open `docs/JAVA_MODULES.md` in a Markdown viewer and check that Modules 1–8 link to Budgetly source files.

---

## 4. Automated Integration Verification

Run the included verification harness (`test/budgetlyDB/BudgetlyIntegrationVerify.java`) against live Apache Derby:

```powershell
& "C:\Program Files\NetBeans-25\netbeans\extide\ant\bin\ant.bat" compile-test
java -cp "build\classes;build\test\classes;dist\lib\derbyclient.jar;dist\lib\derbyshared.jar;dist\lib\derby.jar;dist\lib\derbynet.jar" budgetlyDB.BudgetlyIntegrationVerify
```

---

## 5. Common Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `ClassNotFoundException: ClientDriver` | Missing Derby JARs on classpath | Verify `nbproject/project.properties` includes `JAVADB_DRIVER_LABEL` and `derbyshared.jar`, then run `ant clean jar`. |
| Connection error `08001` on port `1527` | Derby server stopped or blocked | `DatabaseConnection` auto-starts `NetworkServerControl`; ensure no firewall blocks `localhost:1527` and no stale lock holds `AdminDB`. |
| `XSDB6: Another instance of Derby may have already booted` | Two separate processes trying to boot embedded `AdminDB` directly | Always connect via Network Server URL `jdbc:derby://localhost:1527/AdminDB` rather than embedded file path while NetBeans Java DB is running. |
| Account rejected with "currently inactive" | Admin deactivated the user | Sign in as `admin` -> **Admin Dashboard** -> **User Management** -> **Activate / Deactivate**. |
