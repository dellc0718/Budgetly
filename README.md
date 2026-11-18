# BUDGETLY – PERSONAL BUDGET TRACKER

## 1. Project Overview
**BUDGETLY** is a beginner-friendly, academic desktop application written in **Java** using **Java Swing** for the graphical user interface and **Apache Derby (Java DB)** with **JDBC** for relational data persistence.

It is designed so students and instructors can easily read, explain, maintain, and present the codebase while demonstrating all required Object-Oriented Programming (OOP) principles, Java Collections, Generics, Exception Handling, Multithreading (`SwingWorker`), JDBC `PreparedStatement` queries, and in-application learning documentation (User Manual + Java Modules 1–8 Implementation Guide).

---

## 2. Project Objectives
1. Provide a clean, functional personal budget tracker with two distinct roles (**Administrator** and **Regular User**).
2. Demonstrate core Java and OOP concepts (**Classes & Objects, Encapsulation, Inheritance, Polymorphism, Abstraction, Interfaces, Generics, Enums, Wrapper Classes, Collections, Exception Handling, Multithreading, Swing GUI, and JDBC**) through real application functionality.
3. Keep the entire architecture concise (**20 declared Java types**, under 3,000 lines of Java code) so every class has a clear purpose and maps cleanly to a UML class diagram and system flowcharts.
4. Provide an integrated **Settings & Documentation** module featuring **Profile Settings**, a complete **User Manual**, and the **Java Modules 1–8 Implementation Guide** with real source code from the project.

---

## 3. Technologies Used
- **Programming Language**: Java (JDK 17+ / JDK 21)
- **Graphical User Interface**: Java Swing (`JFrame`, `JPanel`, `JTabbedPane`, `CardLayout`, `JTable`, `JScrollPane`, `JButton`, `JTextField`, `JPasswordField`, `JComboBox`, `JProgressBar`, `JTextArea`, `JOptionPane`)
- **Database**: Apache Derby (Java DB) Network Server / Derby Client (`lib/derbyclient.jar`, `lib/derby.jar`, `lib/derbynet.jar`, `lib/derbyshared.jar`, `lib/derbytools.jar`)
- **Database Connectivity**: JDBC (`DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`, `SQLException`)
- **Password Security**: Java Cryptography (`PBKDF2WithHmacSHA256` with a 16-byte `SecureRandom` salt and constant-time `MessageDigest.isEqual` verification)

---

## 4. Application Features

### Authentication & Account Management
- **Public Registration**: Creates a `RegularUser` account (never an Administrator) after validating Full Name, unique Username, Password length, and Password confirmation.
- **Role-Based Login**: Authenticates credentials against salted PBKDF2 hashes, blocks inactive accounts, and redirects `Admin` users to `AdminDashboard` and `RegularUser` accounts to `MainFrame`.

### Administrator Module (`AdminDashboard`)
- View system statistics (**Registered Users**, **Active Users**, **Inactive Users**, **Total Transactions**).
- View all registered users in a `JTable` (passwords are never exposed).
- Search users by username or full name using `BudgetService.search`.
- View selected user account details.
- Activate or deactivate regular user accounts.
- Delete regular user accounts (with confirmation) inside an atomic JDBC transaction that removes their transactions, budgets, and user record together.
- Access Admin Profile Settings and Documentation.

### Regular User Module (`MainFrame`)
1. **Dashboard**: Displays **Total Income**, **Total Expenses**, **Current Balance** (`Total Income - Total Expenses`), **Number of Transactions**, and the **Latest 10 Recent Transactions**.
2. **Income Management**: Add, edit, delete, view, search, and sort income records (`Income ID`, `Description`, `Amount`, `Category`, `Date`).
3. **Expense Management**: Add, edit, delete, view, search, and sort expense records (`Expense ID`, `Description`, `Amount`, `Category`, `Date`) with automatic monthly budget alerts.
4. **Budget Management**: Set and update monthly spending limits, view total monthly expenses and remaining budget, and monitor budget usage via a progress bar and warning notifications.
5. **Transaction History**: View all income and expenses in a unified `JTable` with keyword search, transaction type filter (`All`, `INCOME`, `EXPENSE`), and sorting by date or amount.
6. **Settings & Documentation**:
   - **Profile Settings**: Update Full Name, Username (with uniqueness check), and Email Address; change password (verifies current password and applies a new salt); view read-only Role, Status, and Registration Date.
   - **Documentation & Learning Guide**: Browse the built-in **User Manual** and **Java Modules 1–8 Implementation Guide** displaying real source code from BUDGETLY.

---

## 5. Package & Project Structure

```text
Budgetly/
├── src/
│   └── budgetly/
│       ├── Main.java
│       ├── database/
│       │   └── DatabaseConnection.java
│       ├── model/
│       │   ├── Manageable.java
│       │   ├── UserRole.java
│       │   ├── TransactionType.java
│       │   ├── User.java
│       │   ├── Admin.java
│       │   ├── RegularUser.java
│       │   ├── Transaction.java
│       │   ├── Income.java
│       │   ├── Expense.java
│       │   └── Budget.java
│       ├── dao/
│       │   ├── UserDAO.java
│       │   ├── TransactionDAO.java
│       │   └── BudgetDAO.java
│       ├── service/
│       │   ├── AuthService.java
│       │   └── BudgetService.java
│       └── gui/
│           ├── LoginFrame.java
│           ├── MainFrame.java
│           └── AdminDashboard.java
├── lib/
│   ├── derbyclient.jar
│   ├── derby.jar
│   ├── derbynet.jar
│   ├── derbyshared.jar
│   └── derbytools.jar
├── resources/
│   └── logo.png
├── database/
│   └── schema.sql
└── documentation/
    ├── README.md
    ├── USER_MANUAL.md
    ├── CLASS_DIAGRAM.md
    ├── FLOWCHART.md
    ├── MODULE_1.md
    ├── MODULE_2.md
    ├── MODULE_3.md
    ├── MODULE_4.md
    ├── MODULE_5.md
    ├── MODULE_6.md
    ├── MODULE_7.md
    └── MODULE_8.md
```

---

## 6. Database Setup & Initial Administrator Setup
- **JDBC URL**: `jdbc:derby://localhost:1527/BudgetlyDB;create=true`
- **Database Credentials**: `budgetly` / `budgetly`
- **Automatic Setup**:
  1. When `budgetly.Main` starts, `DatabaseConnection.getConnection()` checks if the Apache Derby Network Server is listening on `localhost:1527` and starts it automatically if needed.
  2. `DatabaseConnection.initializeDatabase()` checks if the `USERS` table exists and executes `database/schema.sql` to create the `USERS`, `TRANSACTIONS`, and `BUDGETS` tables on first run.
  3. `AuthService.initializeDefaultAdmin()` checks if an administrator account exists and creates the initial administrator account with a salted PBKDF2 password hash:
     - **Username**: `admin`
     - **Password**: `admin123`

---

## 7. How to Compile and Run the Application

### Option A: Command Line (Windows PowerShell / Command Prompt)
```bat
cd Budgetly
mkdir bin
javac -cp ".;lib/*" -d bin src/budgetly/Main.java src/budgetly/database/*.java src/budgetly/model/*.java src/budgetly/dao/*.java src/budgetly/service/*.java src/budgetly/gui/*.java
java -cp "bin;lib/*" budgetly.Main
```

### Option B: NetBeans / IntelliJ IDEA / Eclipse
1. Open or import the `Budgetly` folder as a Java project.
2. Add all JAR files inside `lib/` (`derbyclient.jar`, `derby.jar`, `derbynet.jar`, `derbyshared.jar`, `derbytools.jar`) to the project's Libraries / Classpath.
3. Set the working directory to the `Budgetly` folder and run `budgetly.Main`.

---

## 8. Where Required OOP & Java Concepts Are Demonstrated

| Concept | Where Implemented in BUDGETLY |
|---------|-------------------------------|
| **Classes & Objects** | `User`, `Admin`, `RegularUser`, `Transaction`, `Income`, `Expense`, `Budget` in `budgetly.model` |
| **Encapsulation** | Private fields with public getters/setters in `User`, `Transaction`, and `Budget`; password hashes excluded from `User` |
| **Inheritance** | `Admin extends User`, `RegularUser extends User`, `Income extends Transaction`, `Expense extends Transaction` |
| **Polymorphism** | Overridden `canManageUsers()`, `getRole()`, `getDashboardTitle()` on `User`; overridden `getSignedAmount()` and `getType()` on `Transaction` |
| **Abstraction** | Abstract classes `User` and `Transaction` with abstract methods implemented by concrete subclasses |
| **Interfaces** | `Manageable` interface (`matches`, `getSummary`) implemented by both `User` and `Transaction` |
| **Generics** | `BudgetService.search(List<T> items, String keyword)` (`<T extends Manageable>`) and `BudgetService.sort(List<T> items, Comparator<T> comparator)` |
| **Enums** | `UserRole` (`ADMIN`, `USER`) and `TransactionType` (`INCOME`, `EXPENSE` with category arrays) |
| **Wrapper Classes** | `Integer` (`transactionId`, `budgetId`), `Boolean` (`User.isActive()`), `Double` (`Budget.getUsagePercentage()`), `Long` (`UserDAO.getStatistics()`) |
| **Java Collections** | `ArrayList`, `List`, `LinkedHashMap`, `Map`, `Collections.sort`, and `Comparator` across DAOs, Services, and GUIs |
| **Multithreading** | `SwingWorker` in `MainFrame.refreshAllData()` and `AdminDashboard.refreshUsersAndStats()`, plus `SwingUtilities.invokeLater` in `Main` |
| **JDBC & Derby Client** | `DatabaseConnection`, `UserDAO`, `TransactionDAO`, and `BudgetDAO` using `PreparedStatement`, `ResultSet`, and atomic transactions |
