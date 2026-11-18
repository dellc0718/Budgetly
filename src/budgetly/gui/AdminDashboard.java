package budgetly.gui;

import budgetly.model.User;
import budgetly.model.UserRole;
import budgetly.service.AuthService;
import budgetly.service.BudgetService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Administrator Dashboard for managing registered users and viewing system statistics.
 * Enforces administrator privileges and never exposes user passwords.
 */
public class AdminDashboard extends JFrame {

    private final User adminUser;
    private final AuthService authService;

    private List<User> allUsers = new ArrayList<>();
    private final JLabel headerLabel = new JLabel();

    // System statistics labels
    private final JLabel statRegisteredLabel = MainFrame.createCardValueLabel();
    private final JLabel statActiveLabel = MainFrame.createCardValueLabel();
    private final JLabel statInactiveLabel = MainFrame.createCardValueLabel();
    private final JLabel statTransactionsLabel = MainFrame.createCardValueLabel();

    // User management table and search
    private final JTextField searchField = new JTextField(18);
    private final DefaultTableModel userTableModel = MainFrame.createReadOnlyModel(
            new String[]{"User ID", "Full Name", "Username", "Email", "Role", "Status", "Created At"});
    private final JTable userTable = MainFrame.styleTable(new JTable(userTableModel));

    public AdminDashboard(User adminUser, AuthService authService) {
        if (adminUser == null || !adminUser.canManageUsers()) {
            throw new SecurityException("Unauthorized access: Administrator role required.");
        }
        this.adminUser = adminUser;
        this.authService = authService;

        setTitle("BUDGETLY - " + adminUser.getDashboardTitle());
        setSize(1120, 720);
        setMinimumSize(new Dimension(980, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        LoginFrame.applyWindowIcon(this);

        setLayout(new BorderLayout());
        add(buildTopHeader(), BorderLayout.NORTH);

        JTabbedPane adminTabs = new JTabbedPane();
        adminTabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        adminTabs.addTab("1. User Management & System Statistics", buildUserManagementTab());
        adminTabs.addTab("2. Admin Settings & Documentation",
                MainFrame.createSettingsAndDocsPanel(adminUser, authService, this::updateHeaderText));

        add(adminTabs, BorderLayout.CENTER);
        updateHeaderText();
        refreshUsersAndStats();
    }

    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(LoginFrame.BRAND_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        ImageIcon logo = LoginFrame.loadLogoIcon(48, 48);
        if (logo != null) {
            left.add(new JLabel(logo));
        }
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        headerLabel.setForeground(Color.WHITE);
        left.add(headerLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JButton refreshBtn = LoginFrame.createSecondaryButton("Refresh");
        JButton logoutBtn = LoginFrame.createPrimaryButton("Logout");
        logoutBtn.setBackground(new Color(180, 52, 52));

        refreshBtn.addActionListener(e -> refreshUsersAndStats());
        logoutBtn.addActionListener(e -> handleLogout());

        right.add(refreshBtn);
        right.add(logoutBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void updateHeaderText() {
        headerLabel.setText("BUDGETLY " + adminUser.getDashboardTitle()
                + "  |  Signed in as " + adminUser.getFullName() + " (@" + adminUser.getUsername() + ")");
    }

    private JPanel buildUserManagementTab() {
        JPanel page = MainFrame.createPagePanel("Registered Users & System Statistics");

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        statsGrid.setOpaque(false);
        statsGrid.add(MainFrame.createStatCard("Registered Users", statRegisteredLabel, new Color(24, 90, 157)));
        statsGrid.add(MainFrame.createStatCard("Active Users", statActiveLabel, new Color(34, 139, 74)));
        statsGrid.add(MainFrame.createStatCard("Inactive Users", statInactiveLabel, new Color(192, 57, 43)));
        statsGrid.add(MainFrame.createStatCard("Total Transactions", statTransactionsLabel, new Color(115, 81, 16)));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        searchBar.setBackground(Color.WHITE);
        JButton searchBtn = LoginFrame.createPrimaryButton("Search Users");
        JButton showAllBtn = LoginFrame.createSecondaryButton("Show All");
        searchBtn.addActionListener(e -> populateUserTable());
        searchField.addActionListener(e -> populateUserTable());
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            populateUserTable();
        });
        searchBar.add(new JLabel("Search by Username or Full Name:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(showAllBtn);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actionBar.setBackground(Color.WHITE);
        JButton viewInfoBtn = LoginFrame.createSecondaryButton("View Account Info");
        JButton toggleActiveBtn = LoginFrame.createPrimaryButton("Activate / Deactivate");
        JButton deleteUserBtn = LoginFrame.createSecondaryButton("Delete User Account");

        viewInfoBtn.addActionListener(e -> handleViewUserInfo());
        toggleActiveBtn.addActionListener(e -> handleToggleActive());
        deleteUserBtn.addActionListener(e -> handleDeleteUser());

        actionBar.add(viewInfoBtn);
        actionBar.add(toggleActiveBtn);
        actionBar.add(deleteUserBtn);

        JPanel tableBox = new JPanel(new BorderLayout(0, 8));
        tableBox.setBackground(Color.WHITE);
        tableBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Registered User Accounts (Passwords Hidden)"),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        tableBox.add(searchBar, BorderLayout.NORTH);
        tableBox.add(new JScrollPane(userTable), BorderLayout.CENTER);
        tableBox.add(actionBar, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(statsGrid, BorderLayout.NORTH);
        center.add(tableBox, BorderLayout.CENTER);

        page.add(center, BorderLayout.CENTER);
        return page;
    }

    /**
     * Loads registered users and system statistics on a background thread using SwingWorker.
     */
    public final void refreshUsersAndStats() {
        new SwingWorker<Map<String, Long>, Void>() {
            private List<User> loadedUsers;

            @Override
            protected Map<String, Long> doInBackground() throws Exception {
                loadedUsers = authService.getAllUsers(adminUser);
                return authService.getStatistics(adminUser);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Long> stats = get();
                    allUsers = loadedUsers;
                    statRegisteredLabel.setText(String.valueOf(stats.getOrDefault("Registered Users", 0L)));
                    statActiveLabel.setText(String.valueOf(stats.getOrDefault("Active Users", 0L)));
                    statInactiveLabel.setText(String.valueOf(stats.getOrDefault("Inactive Users", 0L)));
                    statTransactionsLabel.setText(String.valueOf(stats.getOrDefault("Total Transactions", 0L)));
                    populateUserTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminDashboard.this,
                            "Error loading users: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void populateUserTable() {
        List<User> filtered = BudgetService.search(allUsers, searchField.getText());
        userTableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (User u : filtered) {
            userTableModel.addRow(new Object[]{
                    u.getUserId(),
                    u.getFullName(),
                    u.getUsername(),
                    u.getEmail() == null ? "-" : u.getEmail(),
                    u.getRole().getLabel(),
                    u.getStatusText(),
                    u.getCreatedAt() == null ? "-" : u.getCreatedAt().format(fmt)
            });
        }
    }

    private User getSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a user row in the table first.",
                    "No User Selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int id = (Integer) userTableModel.getValueAt(row, 0);
        for (User u : allUsers) {
            if (u.getUserId() == id) {
                return u;
            }
        }
        return null;
    }

    private void handleViewUserInfo() {
        User u = getSelectedUser();
        if (u == null) {
            return;
        }
        String details = "Summary: " + u.getSummary()
                + "\nUser ID: " + u.getUserId()
                + "\nFull Name: " + u.getFullName()
                + "\nUsername: " + u.getUsername()
                + "\nEmail: " + (u.getEmail() == null ? "Not provided" : u.getEmail())
                + "\nRole: " + u.getRole().getLabel()
                + "\nStatus: " + u.getStatusText()
                + "\nCreated At: " + u.getCreatedAt();
        JOptionPane.showMessageDialog(this, details,
                "User Account Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleToggleActive() {
        User target = getSelectedUser();
        if (target == null) {
            return;
        }
        if (target.getRole() == UserRole.ADMIN) {
            JOptionPane.showMessageDialog(this,
                    "Administrator accounts cannot be deactivated.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean nextStatus = !Boolean.TRUE.equals(target.isActive());
        String verb = nextStatus ? "activate" : "deactivate";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + verb + " " + target.getSummary() + "?",
                "Confirm Account Status Change", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (authService.setUserActive(adminUser, target.getUserId(), nextStatus)) {
                refreshUsersAndStats();
                JOptionPane.showMessageDialog(this,
                        "Account status updated to " + (nextStatus ? "Active" : "Inactive") + ".",
                        "Status Updated", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteUser() {
        User target = getSelectedUser();
        if (target == null) {
            return;
        }
        if (target.getRole() == UserRole.ADMIN) {
            JOptionPane.showMessageDialog(this,
                    "Administrator accounts cannot be deleted.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete user account " + target.getSummary()
                        + " and all of their transactions and budgets?\nThis action cannot be undone.",
                "Confirm Delete User", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (authService.deleteUser(adminUser, target.getUserId())) {
                refreshUsersAndStats();
                JOptionPane.showMessageDialog(this,
                        "User account deleted successfully.",
                        "User Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error while deleting user: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Do you want to sign out of the Administrator Panel?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginFrame(authService).setVisible(true);
            dispose();
        }
    }
}
