# BUDGETLY – SYSTEM FLOWCHARTS

This document contains the seven classroom-ready Mermaid flowcharts for **BUDGETLY – Personal Budget Tracker**.

---

## 1. Application Startup Flowchart

```mermaid
flowchart TD
    A(["Start Application (Main.main)"]) --> B["Start Swing Event Dispatch Thread (invokeLater)"]
    B --> C["Apply System Look and Feel"]
    C --> D["Ensure Apache Derby Network Server is running on localhost:1527"]
    D --> E{"Does USERS table exist?"}
    E -- "No" --> F["Execute database/schema.sql to create USERS, TRANSACTIONS, BUDGETS"]
    E -- "Yes" --> G{"Does an ADMIN user exist?"}
    F --> G
    G -- "No" --> H["Create default Admin account (admin / PBKDF2 salted hash)"]
    G -- "Yes" --> I["Open LoginFrame Window"]
    H --> I
    I --> J(["Wait for User Action"])
```

---

## 2. User Registration Flowchart

```mermaid
flowchart TD
    A(["User Clicks 'Create a New Account'"]) --> B["Switch LoginFrame CardLayout to REGISTER"]
    B --> C["User enters Full Name, Username, Password, Confirm Password"]
    C --> D["User clicks 'Register Account'"]
    D --> E{"Are Full Name & Username valid?"}
    E -- "No" --> F["Show Validation Warning (JOptionPane)"]
    F --> C
    E -- "Yes" --> G{"Is Password >= 6 chars & matches Confirm?"}
    G -- "No" --> F
    G -- "Yes" --> H{"Does Username already exist in USERS?"}
    H -- "Yes" --> I["Show 'Username is already taken' Warning"]
    I --> C
    H -- "No" --> J["Generate 16-byte random salt & hash password with PBKDF2"]
    J --> K["INSERT INTO users with role = 'USER'"]
    K --> L["Show Success Message & Switch to LOGIN Card"]
    L --> M(["Registration Complete"])
```

---

## 3. User Login Flowchart

```mermaid
flowchart TD
    A(["User Enters Username & Password"]) --> B["User clicks 'Sign In'"]
    B --> C{"Are Username & Password non-empty?"}
    C -- "No" --> D["Show Validation Warning"]
    D --> A
    C -- "Yes" --> E["Query UserDAO.findByUsername(username)"]
    E --> F{"User found & PBKDF2 hash matches?"}
    F -- "No" --> G["Show 'Invalid username or password' Error"]
    G --> A
    F -- "Yes" --> H{"Is user.isActive() true?"}
    H -- "No" --> I["Show 'Account has been deactivated' Warning"]
    I --> A
    H -- "Yes" --> J{"Does user.canManageUsers() return true?"}
    J -- "Yes (Admin)" --> K["Open AdminDashboard & Close LoginFrame"]
    J -- "No (RegularUser)" --> L["Open MainFrame (User Dashboard) & Close LoginFrame"]
    K --> M(["Session Active"])
    L --> M
```

---

## 4. Admin Dashboard and User Management Flowchart

```mermaid
flowchart TD
    A(["Admin Opens AdminDashboard"]) --> B["SwingWorker loads Users & System Statistics in background"]
    B --> C["Update Statistics Cards & Registered Users JTable on EDT"]
    C --> D{"Admin Selects Action"}
    D -- "Search Users" --> E["Filter List<User> via BudgetService.search(allUsers, keyword)"]
    E --> C
    D -- "View Account Info" --> F["Display Selected User Summary in JOptionPane (No Password Shown)"]
    F --> D
    D -- "Activate / Deactivate" --> G{"Is selected user a Regular User?"}
    G -- "No (Admin)" --> H["Show 'Admin accounts cannot be deactivated'"]
    G -- "Yes" --> I["Confirm Action -> UserDAO.setActive(userId, nextStatus)"]
    I --> B
    D -- "Delete User Account" --> J{"Is selected user a Regular User?"}
    J -- "No (Admin)" --> K["Show 'Admin accounts cannot be deleted'"]
    J -- "Yes" --> L["Confirm Delete -> Delete Transactions, Budgets & User in 1 DB Transaction"]
    L --> B
    D -- "Logout" --> M(["Confirm Logout -> Open LoginFrame"])
```

---

## 5. User Dashboard Flowchart

```mermaid
flowchart TD
    A(["Regular User Logs In to MainFrame"]) --> B["Start SwingWorker (refreshAllData)"]
    B --> C["Background Thread: TransactionDAO.findByUser(userId)"]
    C --> D["EDT (done): Calculate Total Income, Total Expenses, Balance"]
    D --> E["Update 4 Summary Cards & Latest 10 Recent Transactions JTable"]
    E --> F{"User Clicks Sidebar Navigation"}
    F -- "1. Dashboard" --> G["Show DASHBOARD Card"]
    F -- "2. Income" --> H["Show INCOME Card"]
    F -- "3. Expenses" --> I["Show EXPENSES Card"]
    F -- "4. Budget" --> J["Show BUDGET Card"]
    F -- "5. Transaction History" --> K["Show HISTORY Card"]
    F -- "6. Settings & Docs" --> L["Show SETTINGS Card (Profile Settings / User Manual / Modules 1-8)"]
    F -- "7. Logout" --> M(["Confirm Logout -> Return to LoginFrame"])
```

---

## 6. Income and Expense Management Flowchart

```mermaid
flowchart TD
    A(["User Opens Income or Expenses Screen"]) --> B{"User Chooses Operation"}
    B -- "Add or Update Record" --> C["Read Description, Amount, Category, Date from Form"]
    C --> D{"Are fields valid (Description non-empty, Amount > 0, Date YYYY-MM-DD)?"}
    D -- "No" --> E["Display Validation Warning (JOptionPane)"]
    D -- "Yes" --> F{"Is this Add or Update?"}
    F -- "Add" --> G["TransactionDAO.insert(tx) with logged-in user_id"]
    F -- "Update" --> H["TransactionDAO.update(tx) WHERE transaction_id = ? AND user_id = ?"]
    G --> I["Refresh Data via SwingWorker & Check Monthly Budget Warning"]
    H --> I
    B -- "Delete Selected" --> J["Confirm Deletion (JOptionPane.YES_NO_OPTION)"]
    J -- "Confirmed" --> K["TransactionDAO.delete(id, userId) & Refresh Data"]
    B -- "Search or Sort" --> L["Run BudgetService.search & sortTransactions -> Update JTable"]
```

---

## 7. Budget Management Flowchart

```mermaid
flowchart TD
    A(["User Opens Budget Management Screen"]) --> B["Select Month (1-12) and Year"]
    B --> C["Query BudgetDAO.findBudget & TransactionDAO.getMonthlyExpenses"]
    C --> D{"User Enters Limit & Clicks 'Save Monthly Budget'?"}
    D -- "Yes" --> E{"Is Monthly Limit a valid number > 0?"}
    E -- "No" --> F["Show Validation Warning"]
    E -- "Yes" --> G{"Does Budget already exist for Month/Year?"}
    G -- "No" --> H["BudgetDAO.insert(new Budget)"]
    G -- "Yes" --> I["BudgetDAO.update(existing Budget)"]
    H --> C
    I --> C
    D -- "View Status" --> J["Calculate Remaining = Monthly Limit - Monthly Expenses"]
    J --> K["Calculate Usage % (Double) & Update JProgressBar"]
    K --> L{"Is Monthly Expenses >= Monthly Limit?"}
    L -- "Yes" --> M["Display Red Warning Banner & Budget Limit Warning Dialog"]
    L -- "No" --> N["Display Green 'Within Monthly Budget' Status"]
```
