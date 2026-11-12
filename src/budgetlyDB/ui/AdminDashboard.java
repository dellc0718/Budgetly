package budgetlyDB.ui;

import budgetly.ui.MainFrame;
import budgetlyDB.AdminDAO;
import budgetlyDB.AuthenticationService;
import budgetlyDB.DatabaseConnection;
import budgetlyDB.DatabaseManager;
import budgetlyDB.SessionManager;
import budgetlyDB.model.ActivityLog;
import budgetlyDB.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Administrator Dashboard (JFrame) displaying user KPIs, recent registrations,
 * recent login activity, audit logs, and quick navigation to User & Database Management.
 */
public class AdminDashboard extends JFrame {

    private final AdminDAO adminDAO = new AdminDAO();
    private final DatabaseManager dbManager = new DatabaseManager();
    private final AuthenticationService authService = new AuthenticationService();
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private JLabel totalUsersVal;
    private JLabel activeUsersVal;
    private JLabel inactiveUsersVal;
    private JLabel totalAdminsVal;
    private JLabel dbBannerStatus;

    private DefaultTableModel recentRegModel;
    private DefaultTableModel recentLoginModel;
    private DefaultTableModel activityLogModel;

    public AdminDashboard() {
        super("Budgetly - Administrator Dashboard");
        SessionManager.requireAdmin();
        BrandTheme.applyAppIcons(this);
        setSize(1140, 720);
        setMinimumSize(new Dimension(960, 620));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);
        add(createCenterContent(), BorderLayout.CENTER);

        refreshDashboardAsync();
    }

    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BrandTheme.SIDEBAR_GREEN);
        top.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        ImageIcon icon = BrandTheme.getMascotBadgeIcon(38);
        if (icon != null) {
            left.add(new JLabel(icon));
        }
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Budgetly Administrator Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Signed in as: " + SessionManager.getCurrentUsername() + " (ADMIN)");
        sub.setFont(BrandTheme.FONT_SMALL);
        sub.setForeground(BrandTheme.CREAM_ACCENT);
        titleBox.add(title);
        titleBox.add(sub);
        left.add(titleBox);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton backToAppBtn = BrandTheme.createSecondaryButton("Budgetly Workspace");
        backToAppBtn.addActionListener(e -> {
            dispose();
            boolean hasVisibleMain = false;
            for (java.awt.Frame f : JFrame.getFrames()) {
                if (f instanceof MainFrame && f.isVisible()) {
                    f.toFront();
                    hasVisibleMain = true;
                    break;
                }
            }
            if (!hasVisibleMain) {
                new MainFrame().setVisible(true);
            }
        });

        JButton logoutBtn = BrandTheme.createDangerButton("Logout");
        logoutBtn.addActionListener(e -> performLogout());

        right.add(backToAppBtn);
        right.add(logoutBtn);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel createCenterContent() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BrandTheme.CONTENT_BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        // Top section: KPI cards + quick action toolbar
        JPanel northSection = new JPanel(new BorderLayout(0, 14));
        northSection.setOpaque(false);

        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiGrid.setOpaque(false);
        totalUsersVal = new JLabel("0");
        activeUsersVal = new JLabel("0");
        inactiveUsersVal = new JLabel("0");
        totalAdminsVal = new JLabel("0");

        kpiGrid.add(createKpiCard("Total Registered Users", totalUsersVal, BrandTheme.EMERALD));
        kpiGrid.add(createKpiCard("Active Users", activeUsersVal, BrandTheme.SUCCESS_GREEN));
        kpiGrid.add(createKpiCard("Inactive Users", inactiveUsersVal, BrandTheme.WARN_AMBER));
        kpiGrid.add(createKpiCard("Total Administrators", totalAdminsVal, BrandTheme.SIDEBAR_GREEN));

        JPanel actionToolbar = BrandTheme.createCardPanel();
        actionToolbar.setLayout(new BorderLayout(12, 0));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);

        JButton userMgmtBtn = BrandTheme.createPrimaryButton("User Management");
        userMgmtBtn.addActionListener(e -> new UserManagementFrame(this::refreshDashboardAsync).setVisible(true));

        JButton dbMgmtBtn = BrandTheme.createPrimaryButton("Database Management");
        dbMgmtBtn.addActionListener(e -> new DatabaseManagementFrame().setVisible(true));

        JButton profileBtn = BrandTheme.createSecondaryButton("Profile & Settings");
        profileBtn.addActionListener(e -> new ProfileSettingsFrame(this::refreshDashboardAsync).setVisible(true));

        JButton docsBtn = BrandTheme.createSecondaryButton("Java Modules Docs");
        docsBtn.addActionListener(e -> new JavaModulesFrame().setVisible(true));

        JButton refreshBtn = BrandTheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshDashboardAsync());

        buttons.add(userMgmtBtn);
        buttons.add(dbMgmtBtn);
        buttons.add(profileBtn);
        buttons.add(docsBtn);
        buttons.add(refreshBtn);

        dbBannerStatus = new JLabel("Checking Derby status...");
        dbBannerStatus.setFont(BrandTheme.FONT_SMALL);
        dbBannerStatus.setForeground(BrandTheme.EMERALD);

        actionToolbar.add(buttons, BorderLayout.WEST);
        actionToolbar.add(dbBannerStatus, BorderLayout.EAST);

        northSection.add(kpiGrid, BorderLayout.NORTH);
        northSection.add(actionToolbar, BorderLayout.SOUTH);

        // Tabs for Overview Tables + Admin Settings
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BrandTheme.FONT_BODY_BOLD);
        tabs.addTab("Overview: Registrations & Logins", createOverviewTablesTab());
        tabs.addTab("Activity Logs (USER_ACTIVITY_LOGS)", createActivityLogsTab());
        tabs.addTab("Admin Settings & Maintenance", createAdminSettingsTab());

        root.add(northSection, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createKpiCard(String title, JLabel valLabel, Color accent) {
        JPanel card = BrandTheme.createCardPanel();
        card.setLayout(new GridLayout(2, 1, 0, 8));
        JLabel t = new JLabel(title);
        t.setFont(BrandTheme.FONT_BODY);
        t.setForeground(BrandTheme.TEXT_MUTED);
        valLabel.setFont(BrandTheme.FONT_KPI);
        valLabel.setForeground(accent);
        card.add(t);
        card.add(valLabel);
        return card;
    }

    private JPanel createOverviewTablesTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(BrandTheme.CONTENT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));

        // Recently Registered
        JPanel regCard = BrandTheme.createCardPanel();
        regCard.setLayout(new BorderLayout(0, 8));
        JLabel regTitle = new JLabel("Recently Registered Users");
        regTitle.setFont(BrandTheme.FONT_SECTION);
        recentRegModel = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Status", "Registered"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable regTable = new JTable(recentRegModel);
        UiLabels.installTableColumn(regTable, 2);
        UiLabels.installTableColumn(regTable, 3);
        BrandTheme.styleTable(regTable);
        regCard.add(regTitle, BorderLayout.NORTH);
        regCard.add(new JScrollPane(regTable), BorderLayout.CENTER);

        // Recent Logins
        JPanel loginCard = BrandTheme.createCardPanel();
        loginCard.setLayout(new BorderLayout(0, 8));
        JLabel loginTitle = new JLabel("Recent Login Activity");
        loginTitle.setFont(BrandTheme.FONT_SECTION);
        recentLoginModel = new DefaultTableModel(new String[]{"ID", "Username", "Role", "Status", "Last Login"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable loginTable = new JTable(recentLoginModel);
        UiLabels.installTableColumn(loginTable, 2);
        UiLabels.installTableColumn(loginTable, 3);
        BrandTheme.styleTable(loginTable);
        loginCard.add(loginTitle, BorderLayout.NORTH);
        loginCard.add(new JScrollPane(loginTable), BorderLayout.CENTER);

        panel.add(regCard);
        panel.add(loginCard);
        return panel;
    }

    private JPanel createActivityLogsTab() {
        JPanel card = BrandTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        JLabel title = new JLabel("Recent System & User Activity Logs");
        title.setFont(BrandTheme.FONT_SECTION);

        activityLogModel = new DefaultTableModel(new String[]{"Log ID", "User", "Type", "Description", "Timestamp"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(activityLogModel);
        BrandTheme.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(460);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        card.add(title, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel createAdminSettingsTab() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2, 16, 0));
        wrapper.setBackground(BrandTheme.CONTENT_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(12, 6, 6, 6));

        // Left: Admin Password Change Card
        JPanel passCard = BrandTheme.createCardPanel();
        passCard.setLayout(new BorderLayout(0, 12));
        JLabel pTitle = new JLabel("Change Administrator Password");
        pTitle.setFont(BrandTheme.FONT_SECTION);
        JLabel pDesc = new JLabel("<html>Update the password for the currently signed-in administrator account. "
                + "For academic presentation security, avoid keeping default demo credentials in production.</html>");
        pDesc.setFont(BrandTheme.FONT_SMALL);
        pDesc.setForeground(BrandTheme.TEXT_MUTED);

        JPanel fields = new JPanel(new GridLayout(6, 1, 0, 6));
        fields.setOpaque(false);
        JPasswordField curPass = new JPasswordField();
        JPasswordField newPass = new JPasswordField();
        JPasswordField confPass = new JPasswordField();
        fields.add(new JLabel("Current Password:"));
        fields.add(curPass);
        fields.add(new JLabel("New Password (min 6 characters):"));
        fields.add(newPass);
        fields.add(new JLabel("Confirm New Password:"));
        fields.add(confPass);

        JButton updatePassBtn = BrandTheme.createPrimaryButton("Change Admin Password");
        updatePassBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to change the administrator password?",
                    "Confirm Password Change", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) {
                return;
            }
            AuthenticationService.AuthResult res = authService.changePassword(
                    curPass.getPassword(), newPass.getPassword(), confPass.getPassword());
            curPass.setText("");
            newPass.setText("");
            confPass.setText("");
            refreshDashboardAsync();
            JOptionPane.showMessageDialog(this, res.getMessage(),
                    res.isSuccess() ? "Success" : "Error",
                    res.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        });

        JPanel passTop = new JPanel(new GridLayout(2, 1, 0, 6));
        passTop.setOpaque(false);
        passTop.add(pTitle);
        passTop.add(pDesc);
        passCard.add(passTop, BorderLayout.NORTH);
        passCard.add(fields, BorderLayout.CENTER);
        passCard.add(updatePassBtn, BorderLayout.SOUTH);

        // Right: Database Maintenance & Quick Tools
        JPanel maintCard = BrandTheme.createCardPanel();
        maintCard.setLayout(new BorderLayout(0, 12));
        JLabel mTitle = new JLabel("Database Maintenance & Administration");
        mTitle.setFont(BrandTheme.FONT_SECTION);

        JPanel btnGrid = new JPanel(new GridLayout(5, 1, 0, 10));
        btnGrid.setOpaque(false);

        JButton checkConnBtn = BrandTheme.createSecondaryButton("Test Apache Derby Connection");
        checkConnBtn.addActionListener(e -> {
            boolean ok = DatabaseConnection.testConnection();
            JOptionPane.showMessageDialog(this,
                    ok ? "Connected successfully to " + DatabaseConnection.DB_URL
                            : "Unable to connect to Apache Derby.",
                    "Connection Test", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });

        JButton verifySchemaBtn = BrandTheme.createSecondaryButton("Verify & Repair Database Schema");
        verifySchemaBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    "Verify all required tables in schema " + DatabaseConnection.SCHEMA_NAME + "?",
                    "Schema Verification", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                try {
                    String msg = dbManager.verifySchemaIntegrity();
                    refreshDashboardAsync();
                    JOptionPane.showMessageDialog(this, msg, "Verified", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton openUsersBtn = BrandTheme.createPrimaryButton("Open User Management");
        openUsersBtn.addActionListener(e -> new UserManagementFrame(this::refreshDashboardAsync).setVisible(true));

        JButton openDbBtn = BrandTheme.createPrimaryButton("Open Database Management");
        openDbBtn.addActionListener(e -> new DatabaseManagementFrame().setVisible(true));

        JButton openSettingsBtn = BrandTheme.createSecondaryButton("Open Full Profile & Settings");
        openSettingsBtn.addActionListener(e -> new ProfileSettingsFrame(this::refreshDashboardAsync).setVisible(true));

        btnGrid.add(checkConnBtn);
        btnGrid.add(verifySchemaBtn);
        btnGrid.add(openUsersBtn);
        btnGrid.add(openDbBtn);
        btnGrid.add(openSettingsBtn);

        maintCard.add(mTitle, BorderLayout.NORTH);
        maintCard.add(btnGrid, BorderLayout.CENTER);

        wrapper.add(passCard);
        wrapper.add(maintCard);
        return wrapper;
    }

    private void refreshDashboardAsync() {
        SessionManager.requireAdmin();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private AdminDAO.AdminOverviewStats stats;
            private List<User> recentReg;
            private List<User> recentLogin;
            private List<ActivityLog> logs;
            private DatabaseManager.DatabaseStatusInfo dbStatus;

            @Override
            protected Void doInBackground() throws Exception {
                stats = adminDAO.getOverviewStats();
                recentReg = adminDAO.getRecentlyRegisteredUsers(15);
                recentLogin = adminDAO.getRecentLoginUsers(15);
                logs = adminDAO.getRecentActivityLogs(50);
                dbStatus = dbManager.getDatabaseStatus();
                return null;
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    get();
                    totalUsersVal.setText(String.valueOf(stats.getTotalUsers()));
                    activeUsersVal.setText(String.valueOf(stats.getActiveUsers()));
                    inactiveUsersVal.setText(String.valueOf(stats.getInactiveUsers()));
                    totalAdminsVal.setText(String.valueOf(stats.getTotalAdmins()));

                    dbBannerStatus.setText(dbStatus.isConnected()
                            ? "● Derby Connected (" + dbStatus.getTableCount() + " tables)"
                            : "● Derby Disconnected");
                    dbBannerStatus.setForeground(dbStatus.isConnected() ? BrandTheme.EMERALD : BrandTheme.DANGER_RED);

                    recentRegModel.setRowCount(0);
                    for (User u : recentReg) {
                        recentRegModel.addRow(new Object[]{
                                u.getUserId(), u.getUsername(), u.getAccountType(), u.getAccountStatus(),
                                u.getDateCreated() == null ? "-" : dtFormat.format(u.getDateCreated())
                        });
                    }

                    recentLoginModel.setRowCount(0);
                    for (User u : recentLogin) {
                        recentLoginModel.addRow(new Object[]{
                                u.getUserId(), u.getUsername(), u.getAccountType(), u.getAccountStatus(),
                                u.getLastLogin() == null ? "-" : dtFormat.format(u.getLastLogin())
                        });
                    }

                    activityLogModel.setRowCount(0);
                    for (ActivityLog l : logs) {
                        activityLogModel.addRow(new Object[]{
                                l.getLogId(),
                                l.getUsernameSnapshot(),
                                l.getActivityType(),
                                l.getActivityDescription(),
                                l.getActivityDate() == null ? "-" : dtFormat.format(l.getActivityDate())
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Unable to refresh admin dashboard: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void performLogout() {
        int c = JOptionPane.showConfirmDialog(this,
                "Log out of your administrator session and return to Sign In?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) {
            return;
        }
        authService.logout();
        for (java.awt.Frame f : JFrame.getFrames()) {
            if (f != this && f.isDisplayable()) {
                f.dispose();
            }
        }
        dispose();
        new LoginFrame().setVisible(true);
    }
}
