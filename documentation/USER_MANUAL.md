# BUDGETLY - PERSONAL BUDGET TRACKER: USER MANUAL

Welcome to **BUDGETLY**, a desktop personal budget tracker built with Java Swing and Apache Derby (Java DB). This manual explains every screen and feature available in the application.

---

## A. Getting Started

### 1. Application Overview
BUDGETLY helps individuals record their income, track their daily expenses, set monthly spending limits, and monitor their current balance. It supports two account roles:
- **Regular User**: Manages personal income, expenses, monthly budgets, transaction history, and profile settings.
- **Administrator**: Views system-wide statistics and manages registered user accounts (searching, viewing info, activating/deactivating, and deleting accounts).

### 2. System Requirements
- **Java Development Kit (JDK)**: Java 17 or newer (tested on JDK 21).
- **Database**: Apache Derby Network Server / Derby Client (`derbyclient.jar`, `derby.jar`, `derbynet.jar`, `derbyshared.jar`, `derbytools.jar` located in the `lib/` folder).
- **Operating System**: Windows, macOS, or Linux with a desktop display.

### 3. How to Launch the Application
- **From an IDE (NetBeans / IntelliJ IDEA / Eclipse)**:
  Open the `Budgetly` project, ensure all `.jar` files in `lib/` are in the project libraries/classpath, and run `budgetly.Main`.
- **From the Command Prompt / PowerShell**:
  ```
  javac -cp ".;lib/*" -d bin src/budgetly/Main.java src/budgetly/database/*.java src/budgetly/model/*.java src/budgetly/dao/*.java src/budgetly/service/*.java src/budgetly/gui/*.java
  java -cp "bin;lib/*" budgetly.Main
  ```
  On first launch, BUDGETLY automatically starts the local Apache Derby Network Server on port `1527` (if not already running), executes `database/schema.sql`, and initializes the default administrator account.

### 4. How to Register an Account
1. On the **Welcome to BUDGETLY** window, click **Create a New Account**.
2. Enter your **Full Name** (up to 100 characters).
3. Enter a unique **Username** (3 to 20 characters using letters, numbers, or `_`).
4. Enter a **Password** (at least 6 characters) and repeat it in **Confirm Password**.
5. Click **Register Account**. Public registration always creates a **Regular User** account.

### 5. How to Log In
1. Enter your **Username** and **Password** on the Sign In screen.
2. Click **Sign In** (or press Enter).
   - **Regular Users** are redirected to **My Budget Dashboard** (`MainFrame`).
   - **Administrators** are redirected to the **Administrator Panel** (`AdminDashboard`).
   - Default Administrator credentials created on first startup:
     - **Username**: `admin`
     - **Password**: `admin123`

---

## B. Dashboard Guide

Click **1. Dashboard** in the left sidebar to view your financial summary:
- **Total Income**: The sum of all income records saved under your account.
- **Total Expenses**: The sum of all expense records saved under your account.
- **Current Balance**: Calculated as `Total Income - Total Expenses`.
- **Transactions**: The total count of your income and expense entries.
- **Recent Transactions (Latest 10)**: A table showing your 10 most recent transactions ordered from newest to oldest.
- Click **Refresh Data** in the top-right bar at any time to reload records from the database in the background.

---

## C. Income Management

Click **2. Income** in the left sidebar to manage money you receive:
1. **Adding Income**:
   - Fill in **Description** (e.g., `Monthly Salary`), **Amount (PHP)** (e.g., `25000.00`), select a **Category** (`Salary`, `Allowance`, `Business`, `Freelance`, `Gift`, `Other`), and enter **Date** in `YYYY-MM-DD` format.
   - Click **Add Income**.
2. **Editing Income**:
   - Click any row in the **Income History** table. Its details are loaded into the form fields above.
   - Modify the description, amount, category, or date, then click **Update Selected**.
3. **Deleting Income**:
   - Select a row in the **Income History** table and click **Delete Selected**.
   - Confirm the deletion in the dialog prompt.
4. **Viewing, Searching, and Sorting Income History**:
   - Type a keyword in the **Search** box and click **Search** to match descriptions or categories.
   - Use the **Sort By** dropdown to sort by `Date (Newest First)`, `Date (Oldest First)`, `Amount (High to Low)`, or `Amount (Low to High)`.

---

## D. Expense Management

Click **3. Expenses** in the left sidebar to manage money you spend:
1. **Adding Expenses**:
   - Enter **Description** (e.g., `Grocery Shopping`), **Amount (PHP)** (e.g., `1850.50`), choose a **Category** (`Food`, `Transportation`, `Bills & Utilities`, `Shopping`, `Education`, `Health`, `Entertainment`, `Other`), and enter **Date** (`YYYY-MM-DD`).
   - Click **Add Expense**. If the new expense causes your monthly spending to reach or exceed your monthly budget, BUDGETLY displays a budget warning notification.
2. **Editing Expenses**:
   - Click an expense row in the **Expense History** table, edit the fields, and click **Update Selected**.
3. **Deleting Expenses**:
   - Select an expense row, click **Delete Selected**, and confirm in the dialog box.
4. **Viewing, Searching, and Sorting Expense History**:
   - Use the **Search** box and **Sort By** dropdown above the table to filter and order your expenses.

---

## E. Budget Management

Click **4. Budget** in the left sidebar to plan and monitor your monthly spending limit:
1. **Creating a Monthly Budget**:
   - Select the **Month** (`1` to `12`) and **Year**, enter a positive amount in **Monthly Limit (PHP)**, and click **Save Monthly Budget**.
2. **Updating a Budget**:
   - Select the month and year you want to modify, enter the new limit in **Monthly Limit (PHP)**, and click **Save Monthly Budget**.
3. **Understanding Remaining Budget**:
   - `Remaining Budget = Monthly Budget Limit - Total Monthly Expenses`.
   - A positive number means you still have money left to spend for that month; a negative number means you have exceeded your monthly limit.
4. **Understanding Budget Warning Notifications**:
   - The **Budget Usage Monitor** displays a progress bar (`0%` to `100%`) and a status message.
   - When `Total Monthly Expenses >= Monthly Budget Limit`, a red warning banner and popup alert notify you that your budget has been reached or exceeded.

---

## F. Transaction History

Click **5. Transaction History** in the left sidebar to inspect all income and expenses together in one table:
- **Viewing Transactions**: Displays `Transaction ID`, `Transaction Type`, `Description`, `Category`, `Amount`, and `Date`.
- **Searching Transactions**: Enter any keyword in **Search** and click **Apply Filter**.
- **Filtering Transaction Types**: Choose `All Types`, `INCOME`, or `EXPENSE` in the **Type** dropdown.
- **Sorting Transactions**: Choose `Date (Newest First)`, `Date (Oldest First)`, `Amount (High to Low)`, or `Amount (Low to High)` in **Sort By**.
- Click **Reset** to restore the full unfiltered list.

---

## G. Profile Settings

Click **6. Settings & Docs** and select the **1. Profile Settings** tab:
1. **Updating Personal Information & Changing Username**:
   - Edit your **Full Name**, **Username**, or **Email Address** and click **Save Personal Information**.
   - If the new username is already taken by another user, BUDGETLY rejects the change and shows an error message.
2. **Changing Password**:
   - In **B. Change Password**, enter your **Current Password**, your **New Password** (at least 6 characters), and **Confirm New Password**.
   - Click **Update Password**. BUDGETLY verifies your current password and hashes the new password using PBKDF2 with a fresh random salt.
3. **Understanding Account Information**:
   - Your **Account Role** (`Regular User` or `Administrator`), **Account Status** (`Active`), and **Registration Date** are displayed as read-only fields. Users cannot change their own role or activation status.

---

## H. Administrator Guide

1. **Administrator Login**:
   - Sign in using an administrator account (default: `admin` / `admin123`). You are taken to the **Administrator Panel**.
2. **Viewing Registered Users & System Statistics**:
   - The top cards show **Registered Users**, **Active Users**, **Inactive Users**, and **Total Transactions**.
   - The table lists all accounts without ever exposing passwords.
3. **Searching Users**:
   - Type a username or full name in **Search by Username or Full Name** and click **Search Users**.
4. **Viewing User Account Info**:
   - Select a row in the table and click **View Account Info** to see the user's profile summary.
5. **Activating and Deactivating Accounts**:
   - Select a regular user row and click **Activate / Deactivate**. Inactive users are blocked from signing in until reactivated.
6. **Deleting Registered Users**:
   - Select a regular user row, click **Delete User Account**, and confirm. BUDGETLY deletes the user along with their transactions and budgets inside a single database transaction.

---

## I. Troubleshooting

- **Incorrect Login Credentials**:
  - *Symptom*: "Invalid username or password. Please try again."
  - *Solution*: Check for typos and Caps Lock. Usernames are case-insensitive, but passwords are case-sensitive.
- **Duplicate Username**:
  - *Symptom*: "The username '...' is already taken."
  - *Solution*: Choose a different username (3–20 letters, numbers, or underscores).
- **Database Connection Failure**:
  - *Symptom*: "Failed to initialize BUDGETLY database..."
  - *Solution*: Verify that `derbyclient.jar`, `derby.jar`, `derbynet.jar`, and `derbyshared.jar` in `lib/` are included on the classpath and port `1527` is not blocked by another program.
- **Invalid Amount**:
  - *Symptom*: "Amount must be a valid number greater than zero (example: 1250.50)."
  - *Solution*: Enter a positive numeric value using digits and an optional decimal point (no currency symbols or letters).
- **Missing Required Fields**:
  - *Symptom*: "Description is required." or "Full name and username are required."
  - *Solution*: Complete all required input fields before clicking Save, Add, or Register.
- **Inactive Account**:
  - *Symptom*: "Your account has been deactivated by an administrator."
  - *Solution*: Contact the system administrator (`admin`) to reactivate your account in the Administrator Panel.
- **Unable to Save Transactions**:
  - *Symptom*: Date validation warning or update error.
  - *Solution*: Ensure the date uses `YYYY-MM-DD` format (e.g., `2026-11-17`) and, when updating, click a row in the table first so a record ID is selected.
