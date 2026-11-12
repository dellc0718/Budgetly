package budgetlyDB;

import budgetly.ui.MainFrame;
import budgetlyDB.model.BudgetTransaction;
import budgetlyDB.model.DashboardSummary;
import budgetlyDB.model.FinancialAccount;
import budgetlyDB.model.User;
import budgetlyDB.model.UserProfile;
import budgetlyDB.model.UserSettings;
import budgetlyDB.ui.AdminDashboard;
import budgetlyDB.ui.DatabaseManagementFrame;
import budgetlyDB.ui.JavaModulesFrame;
import budgetlyDB.ui.LoginFrame;
import budgetlyDB.ui.ProfileSettingsFrame;
import budgetlyDB.ui.SignupFrame;
import budgetlyDB.ui.UserManagementFrame;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * End-to-end integration verification script covering Database, Security,
 * Auth, Data Isolation, Admin Dashboard, Java Modules, and Swing JFrame instantiation.
 */
public class BudgetlyIntegrationVerify {

    private static int passed = 0;

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError("FAILED: " + label);
        }
        passed++;
        System.out.println("[PASS] " + label);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Budgetly Database & Integration Verification ===");
        try {
            runAllTests();
            System.out.println("\nALL " + passed + " CHECKS PASSED SUCCESSFULLY!");
        } finally {
            SessionManager.clearSession();
            DatabaseConnection.shutdown();
        }
    }

    private static void runAllTests() throws Exception {
        // 1. Database Connection & Initialization
        check(DatabaseConnection.testConnection(), "Derby connection reachable");
        DatabaseInitializer.initialize();
        DatabaseInitializer.initialize(); // Idempotent second call
        check(DatabaseInitializer.isInitialized(), "DatabaseInitializer completed idempotently");

        try (Connection c1 = DatabaseConnection.getConnection()) {
            check(c1 != null && !c1.isClosed(), "Pooled connection acquired");
        }

        // 2. Password Hashing verification
        PasswordHasher.HashedPassword h1 = PasswordHasher.hashPassword("secret123".toCharArray());
        PasswordHasher.HashedPassword h2 = PasswordHasher.hashPassword("secret123".toCharArray());
        check(!h1.getSalt().equals(h2.getSalt()), "Unique random salt generated per hash");
        check(!h1.getHash().equals(h2.getHash()), "Identical passwords produce distinct hashes");
        check(PasswordHasher.verifyPassword("secret123".toCharArray(), h1.getHash(), h1.getSalt()),
                "PBKDF2WithHmacSHA256 verification succeeds for valid password");
        check(!PasswordHasher.verifyPassword("wrongpass".toCharArray(), h1.getHash(), h1.getSalt()),
                "PBKDF2WithHmacSHA256 verification rejects invalid password");

        // 3. Default Admin Login & Metadata Inspection
        AuthenticationService auth = new AuthenticationService();
        AuthenticationService.AuthResult adminLogin = auth.login("admin", "admin".toCharArray());
        check(adminLogin.isSuccess(), "Default admin logs in with admin/admin");
        check(SessionManager.isAdmin(), "SessionManager identifies admin role");

        AdminDAO adminDAO = new AdminDAO();
        List<String> tables = adminDAO.listApplicationTables();
        check(tables.contains("USERS") && tables.contains("USER_PROFILES")
                && tables.contains("USER_SETTINGS") && tables.contains("USER_ACTIVITY_LOGS")
                && tables.contains("FINANCIAL_ACCOUNTS") && tables.contains("TRANSACTIONS")
                && tables.contains("MONTHLY_BUDGETS"),
                "All 7 expected Derby tables exist in ADMINUSER schema: " + tables);

        AdminDAO.TablePreview usersPreview = adminDAO.previewTable("USERS", 50);
        int hashColIdx = usersPreview.getColumns().indexOf("PASSWORD_HASH");
        int saltColIdx = usersPreview.getColumns().indexOf("PASSWORD_SALT");
        check(hashColIdx >= 0 && saltColIdx >= 0
                && "[PROTECTED]".equals(usersPreview.getRows().get(0)[hashColIdx])
                && "[PROTECTED]".equals(usersPreview.getRows().get(0)[saltColIdx]),
                "Admin table preview masks PASSWORD_HASH and PASSWORD_SALT");

        DatabaseManager dbManager = new DatabaseManager();
        DatabaseManager.DatabaseStatusInfo status = dbManager.getDatabaseStatus();
        check(status.isConnected() && status.getProductName().toLowerCase().contains("derby"),
                "DatabaseManager reports Apache Derby metadata: " + status.getProductName() + " " + status.getProductVersion());
        auth.logout();
        check(!SessionManager.isLoggedIn(), "Session cleared after logout");

        // 4. Registration & Duplicate / Validation Rejection
        String u1 = "verify_alice_" + System.currentTimeMillis();
        String u2 = "verify_bob_" + System.currentTimeMillis();
        String e1 = u1 + "@example.com";
        String e2 = u2 + "@example.com";

        AuthenticationService.AuthResult regMismatch = auth.registerUser(
                u1, e1, "Alice", "Tester", "pass123".toCharArray(), "diff456".toCharArray());
        check(!regMismatch.isSuccess(), "Registration rejects mismatched passwords");

        AuthenticationService.AuthResult regAlice = auth.registerUser(
                u1, e1, "Alice", "Tester", "alicePass1".toCharArray(), "alicePass1".toCharArray());
        check(regAlice.isSuccess(), "Regular user Alice registered successfully");

        AuthenticationService.AuthResult dupUser = auth.registerUser(
                u1, "other_" + e1, "Alice", "Dup", "alicePass1".toCharArray(), "alicePass1".toCharArray());
        check(!dupUser.isSuccess(), "Duplicate username rejected");
        AuthenticationService.AuthResult dupUserCase = auth.registerUser(
                u1.toUpperCase(), "case_" + e1, "Alice", "Dup", "alicePass1".toCharArray(), "alicePass1".toCharArray());
        check(!dupUserCase.isSuccess() && dupUserCase.getMessage().startsWith("Username already exists"),
                "Case-insensitive duplicate username gets a clear message");

        AuthenticationService.AuthResult dupEmail = auth.registerUser(
                u1 + "_alt", e1, "Alice", "DupEmail", "alicePass1".toCharArray(), "alicePass1".toCharArray());
        check(!dupEmail.isSuccess(), "Duplicate email rejected");
        AuthenticationService.AuthResult dupEmailCase = auth.registerUser(
                "mailcase" + System.currentTimeMillis(), e1.toUpperCase(), "Alice", "DupEmail", "alicePass1".toCharArray(), "alicePass1".toCharArray());
        check(!dupEmailCase.isSuccess() && dupEmailCase.getMessage().startsWith("Email address is already registered"),
                "Case-insensitive duplicate email gets a clear message");

        AuthenticationService.AuthResult regBob = auth.registerUser(
                u2, e2, "Bob", "Tester", "bobPass1".toCharArray(), "bobPass1".toCharArray());
        check(regBob.isSuccess(), "Regular user Bob registered successfully");

        // 5. User Login, Authorization & Data Isolation
        AuthenticationService.AuthResult badLogin = auth.login(u1, "wrongPassword".toCharArray());
        check(!badLogin.isSuccess(), "Incorrect password rejected");

        AuthenticationService.AuthResult aliceLogin = auth.login(u1, "alicePass1".toCharArray());
        check(aliceLogin.isSuccess() && !SessionManager.isAdmin(),
                "Alice logged in as non-admin USER");

        boolean adminBlocked = false;
        try {
            adminDAO.getAllUsers();
        } catch (SecurityException ex) {
            adminBlocked = true;
        }
        check(adminBlocked, "Non-admin user blocked from AdminDAO operations");

        // Alice financial workflow
        BudgetService budgetService = new BudgetService();
        List<FinancialAccount> aliceAccounts = budgetService.getUserAccounts();
        check(aliceAccounts.size() == 1 && "Cash Wallet".equals(aliceAccounts.get(0).getAccountName()),
                "Starter Cash Wallet automatically created for Alice");

        int bankId = budgetService.createAccount("BPI Savings", "BANK", new BigDecimal("5000.00"), "Main bank");
        int incomeId = budgetService.recordTransaction(bankId, "INCOME", "Salary", new BigDecimal("12000.00"),
                "Paycheck", LocalDate.now());
        int expId = budgetService.recordTransaction(bankId, "EXPENSE", "Food", new BigDecimal("450.00"),
                "Lunch", LocalDate.now());

        YearMonth ym = YearMonth.now();
        budgetService.saveMonthlyBudget(ym.getYear(), ym.getMonthValue(), "OVERALL", new BigDecimal("10000.00"));

        DashboardSummary aliceDash = budgetService.getDashboardSummary();
        check(aliceDash.getMonthlyIncome().compareTo(new BigDecimal("12000.00")) == 0,
                "Alice monthly income reflects Derby transaction");
        check(aliceDash.getMonthlyExpenses().compareTo(new BigDecimal("450.00")) == 0,
                "Alice monthly expenses reflects Derby transaction");
        check(aliceDash.getTotalAccountBalance().compareTo(new BigDecimal("16550.00")) == 0,
                "Alice total balance reflects 5000 + 12000 - 450 = 16550.00");
        check(budgetService.getMonthlyTrend().size() == 6
                && budgetService.getMonthlyTrend().get(ym)[0].compareTo(new BigDecimal("12000.00")) == 0
                && budgetService.getMonthlyTrend().get(ym)[1].compareTo(new BigDecimal("450.00")) == 0,
                "Six-month chart aggregates current income and expenses with empty months");
        check(budgetService.getMonthlyExpensesByCategory(ym.getYear(), ym.getMonthValue())
                .get("Food").compareTo(new BigDecimal("450.00")) == 0,
                "Expense breakdown uses persisted category totals");
        check(budgetService.updateTransaction(expId, bankId, "EXPENSE", "Food",
                new BigDecimal("500.00"), "Edited lunch", LocalDate.now()),
                "Alice edits her expense transaction");
        check(budgetService.getDashboardSummary().getTotalAccountBalance()
                .compareTo(new BigDecimal("16500.00")) == 0,
                "Editing expense updates linked account balance atomically");
        check(budgetService.updateTransaction(expId, bankId, "EXPENSE", "Food",
                new BigDecimal("450.00"), "Lunch", LocalDate.now()),
                "Alice restores edited expense for deletion check");

        // Test deleting transaction reverses balance
        check(budgetService.deleteTransaction(expId), "Alice deletes expense transaction");
        DashboardSummary afterDelete = budgetService.getDashboardSummary();
        check(afterDelete.getTotalAccountBalance().compareTo(new BigDecimal("17000.00")) == 0,
                "Deleting transaction atomically reversed account balance back to 17000.00");

        // Profile, Settings, Password Change & Persistence across logout/login
        UserProfile ap = SessionManager.getCurrentProfile();
        ap.setDisplayName("Alice T.");
        ap.setContactNumber("0917-555-0101");
        UserSettings as = SessionManager.getCurrentSettings();
        as.setCurrencyPreference("USD");
        as.setTheme("FOREST");
        AuthenticationService.AuthResult profSave = auth.saveCurrentUserProfileAndSettings(
                e1, "Alice", "Tester", ap, as);
        check(profSave.isSuccess(), "Alice saved profile and settings");

        AuthenticationService.AuthResult pwChange = auth.changePassword(
                "alicePass1".toCharArray(), "aliceNewPass9".toCharArray(), "aliceNewPass9".toCharArray());
        check(pwChange.isSuccess(), "Alice changed password");
        auth.logout();

        check(!auth.login(u1, "alicePass1".toCharArray()).isSuccess(),
                "Old password rejected after password change");
        check(auth.login(u1, "aliceNewPass9".toCharArray()).isSuccess(),
                "New password accepted on re-login");
        check("USD".equals(SessionManager.getCurrentSettings().getCurrencyPreference())
                && "FOREST".equals(SessionManager.getCurrentSettings().getTheme())
                && "Alice T.".equals(SessionManager.getCurrentProfile().getDisplayName()),
                "Profile and settings restored after re-login");
        auth.logout();

        // Bob data isolation check
        check(auth.login(u2, "bobPass1".toCharArray()).isSuccess(), "Bob logs in");
        List<FinancialAccount> bobAccounts = budgetService.getUserAccounts();
        List<BudgetTransaction> bobTxs = budgetService.searchTransactions("ALL", "ALL", null, "");
        check(bobAccounts.size() == 1 && bobTxs.isEmpty(),
                "Bob cannot see Alice's BPI Savings account or Alice's transactions");
        check(!budgetService.updateTransaction(incomeId, bobAccounts.get(0).getAccountId(), "EXPENSE",
                "Food", new BigDecimal("1.00"), "Unauthorized", LocalDate.now()),
                "Bob cannot edit Alice's transaction");
        auth.logout();

        // 6. Admin User Management & Cleanup of temporary test accounts
        check(auth.login("admin", "admin".toCharArray()).isSuccess(), "Admin logs back in");
        List<User> searched = adminDAO.searchUsers(u1);
        check(searched.size() == 1 && searched.get(0).getUsername().equals(u1),
                "Admin user search locates Alice");
        int aliceId = searched.get(0).getUserId();

        check(adminDAO.setUserStatus(aliceId, "INACTIVE"), "Admin deactivates Alice");
        auth.logout();
        check(!auth.login(u1, "aliceNewPass9".toCharArray()).isSuccess(),
                "Inactive account cannot log in");

        check(auth.login("admin", "admin".toCharArray()).isSuccess(), "Admin logs in for cleanup");
        int bobId = adminDAO.searchUsers(u2).get(0).getUserId();
        check(adminDAO.deleteUser(aliceId) && adminDAO.deleteUser(bobId),
                "Admin deletes temporary test accounts cleanly");

        AdminDAO.AdminOverviewStats stats = adminDAO.getOverviewStats();
        check(stats.getTotalAdmins() >= 1 && stats.getTotalUsers() >= 1,
                "Admin overview stats reflect current database state");

        // 7. Swing GUI JFrame & Java Modules instantiation smoke test on EDT
        SwingUtilities.invokeAndWait(() -> {
            LoginFrame lf = new LoginFrame();
            check(!lf.getIconImages().isEmpty(), "LoginFrame has Budgetly mascot window icons");
            lf.dispose();

            SignupFrame sf = new SignupFrame();
            check(!sf.getIconImages().isEmpty(), "SignupFrame has Budgetly mascot window icons");
            sf.dispose();

            MainFrame mf = new MainFrame();
            check(!mf.getIconImages().isEmpty(), "MainFrame has Budgetly mascot window icons");
            mf.dispose();

            AdminDashboard ad = new AdminDashboard();
            check(!ad.getIconImages().isEmpty(), "AdminDashboard has Budgetly mascot window icons");
            ad.dispose();

            UserManagementFrame um = new UserManagementFrame();
            um.dispose();

            DatabaseManagementFrame dm = new DatabaseManagementFrame();
            dm.dispose();

            ProfileSettingsFrame ps = new ProfileSettingsFrame();
            ps.dispose();

            JavaModulesFrame jm = new JavaModulesFrame();
            check(JavaModulesFrame.findDocumentation().isFile(), "External Java Modules Markdown guide available");
            jm.dispose();
        });
        Thread.sleep(300);
        auth.logout();
    }
}
