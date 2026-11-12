package budgetlyDB.ui;

import budgetlyDB.ActivityLogDAO;
import budgetlyDB.AdminDAO;
import budgetlyDB.ProfileDAO;
import budgetlyDB.SessionManager;
import budgetlyDB.model.ActivityLog;
import budgetlyDB.model.User;
import budgetlyDB.model.UserProfile;
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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Administrator User Management JFrame.
 * Allows searching, viewing, editing, activating/deactivating, and deleting users.
 */
public class UserManagementFrame extends JFrame {

    private final AdminDAO adminDAO = new AdminDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final Runnable onDataChangedCallback;

    private JTextField searchField;
    private DefaultTableModel userTableModel;
    private JTable userTable;
    private JLabel countLabel;

    public UserManagementFrame() {
        this(null);
    }

    public UserManagementFrame(Runnable onDataChangedCallback) {
        super("Budgetly Admin - User Management");
        SessionManager.requireAdmin();
        this.onDataChangedCallback = onDataChangedCallback;
        BrandTheme.applyAppIcons(this);
        setSize(1060, 660);
        setMinimumSize(new Dimension(900, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createHeaderBar(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        loadUsersAsync("");
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BrandTheme.SIDEBAR_GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        ImageIcon icon = BrandTheme.getMascotBadgeIcon(34);
        if (icon != null) {
            left.add(new JLabel(icon));
        }
        JLabel title = new JLabel("User Management Console");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        left.add(title);

        JButton closeBtn = BrandTheme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        header.add(left, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createMainPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BrandTheme.CONTENT_BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Search bar card
        JPanel searchBar = BrandTheme.createCardPanel();
        searchBar.setLayout(new BorderLayout(12, 0));
        JPanel searchLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchLeft.setOpaque(false);
        searchLeft.add(new JLabel("Search (username, email, or name):"));
        searchField = new JTextField(24);
        searchField.setFont(BrandTheme.FONT_BODY);
        searchField.setPreferredSize(new Dimension(240, 34));
        searchField.addActionListener(e -> loadUsersAsync(searchField.getText().trim()));
        JButton searchBtn = BrandTheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> loadUsersAsync(searchField.getText().trim()));
        JButton showAllBtn = BrandTheme.createSecondaryButton("Show All");
        showAllBtn.addActionListener(e -> {
            searchField.setText("");
            loadUsersAsync("");
        });
        searchLeft.add(searchField);
        searchLeft.add(searchBtn);
        searchLeft.add(showAllBtn);

        countLabel = new JLabel("Loading users...");
        countLabel.setFont(BrandTheme.FONT_BODY_BOLD);
        countLabel.setForeground(BrandTheme.EMERALD);

        searchBar.add(searchLeft, BorderLayout.WEST);
        searchBar.add(countLabel, BorderLayout.EAST);

        // Table Card
        JPanel tableCard = BrandTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 12));

        String[] cols = {"User ID", "Username", "Full Name", "Email", "Role", "Status", "Created", "Last Login"};
        userTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(userTableModel);
        UiLabels.installTableColumn(userTable, 4);
        UiLabels.installTableColumn(userTable, 5);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        BrandTheme.styleTable(userTable);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(65);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(190);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        userTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        userTable.getColumnModel().getColumn(6).setPreferredWidth(135);
        userTable.getColumnModel().getColumn(7).setPreferredWidth(135);

        JScrollPane sp = new JScrollPane(userTable);
        tableCard.add(sp, BorderLayout.CENTER);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);

        JButton viewBtn = BrandTheme.createSecondaryButton("View User Info");
        viewBtn.addActionListener(e -> viewSelectedUser());

        JButton editBtn = BrandTheme.createPrimaryButton("Edit User");
        editBtn.addActionListener(e -> editSelectedUser());

        JButton toggleStatusBtn = BrandTheme.createSecondaryButton("Activate / Deactivate");
        toggleStatusBtn.addActionListener(e -> toggleSelectedUserStatus());

        JButton deleteBtn = BrandTheme.createDangerButton("Delete User");
        deleteBtn.addActionListener(e -> deleteSelectedUser());

        JButton refreshBtn = BrandTheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadUsersAsync(searchField.getText().trim()));

        actions.add(viewBtn);
        actions.add(editBtn);
        actions.add(toggleStatusBtn);
        actions.add(deleteBtn);
        actions.add(refreshBtn);

        tableCard.add(actions, BorderLayout.SOUTH);

        root.add(searchBar, BorderLayout.NORTH);
        root.add(tableCard, BorderLayout.CENTER);
        return root;
    }

    private void loadUsersAsync(String keyword) {
        SessionManager.requireAdmin();
        countLabel.setText("Loading...");
        SwingWorker<List<User>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<User> doInBackground() throws Exception {
                return keyword.isEmpty() ? adminDAO.getAllUsers() : adminDAO.searchUsers(keyword);
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    List<User> users = get();
                    userTableModel.setRowCount(0);
                    for (User u : users) {
                        userTableModel.addRow(new Object[]{
                                u.getUserId(),
                                u.getUsername(),
                                u.getFullName(),
                                u.getEmail(),
                                u.getAccountType(),
                                u.getAccountStatus(),
                                u.getDateCreated() == null ? "-" : dtFormat.format(u.getDateCreated()),
                                u.getLastLogin() == null ? "Never" : dtFormat.format(u.getLastLogin())
                        });
                    }
                    countLabel.setText("Showing " + users.size() + " user(s)");
                } catch (Exception ex) {
                    countLabel.setText("Error loading users");
                    JOptionPane.showMessageDialog(UserManagementFrame.this,
                            "Failed to load users: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private Integer getSelectedUserId() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user row first.", "No Selection",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return (Integer) userTableModel.getValueAt(row, 0);
    }

    private void viewSelectedUser() {
        Integer userId = getSelectedUserId();
        if (userId == null) {
            return;
        }
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                User u = adminDAO.getUserById(userId);
                if (u == null) {
                    return "User not found.";
                }
                UserProfile p = profileDAO.findByUserId(userId);
                List<ActivityLog> logs = activityLogDAO.findLogsForUser(userId, 5);
                StringBuilder sb = new StringBuilder();
                sb.append("User ID: ").append(u.getUserId()).append("\n");
                sb.append("Username: ").append(u.getUsername()).append("\n");
                sb.append("Full Name: ").append(u.getFullName()).append("\n");
                sb.append("Email: ").append(u.getEmail()).append("\n");
                sb.append("Account Type: ").append(UiLabels.label(u.getAccountType())).append("\n");
                sb.append("Status: ").append(UiLabels.label(u.getAccountStatus())).append("\n");
                sb.append("Created: ").append(u.getDateCreated() == null ? "-" : dtFormat.format(u.getDateCreated())).append("\n");
                sb.append("Last Login: ").append(u.getLastLogin() == null ? "Never" : dtFormat.format(u.getLastLogin())).append("\n");
                if (p != null) {
                    sb.append("\n--- Saved Profile ---\n");
                    sb.append("Display Name: ").append(p.getDisplayName()).append("\n");
                    sb.append("Contact: ").append(p.getContactNumber()).append("\n");
                    sb.append("Occupation: ").append(p.getOccupation()).append("\n");
                    sb.append("Address: ").append(p.getAddress()).append("\n");
                }
                sb.append("\n--- Recent Activity ---\n");
                if (logs.isEmpty()) {
                    sb.append("No logged events yet.\n");
                } else {
                    for (ActivityLog l : logs) {
                        sb.append("• [").append(l.getActivityType()).append("] ")
                                .append(l.getActivityDescription()).append("\n");
                    }
                }
                return sb.toString();
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    JOptionPane.showMessageDialog(UserManagementFrame.this, get(),
                            "User Details", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserManagementFrame.this,
                            "Error reading user details: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void editSelectedUser() {
        Integer userId = getSelectedUserId();
        if (userId == null) {
            return;
        }
        try {
            User target = adminDAO.getUserById(userId);
            if (target == null) {
                JOptionPane.showMessageDialog(this, "User no longer exists.", "Not Found", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JTextField emailTf = new JTextField(target.getEmail());
            JTextField firstTf = new JTextField(target.getFirstName());
            JTextField lastTf = new JTextField(target.getLastName());
            JComboBox<String> typeCb = new JComboBox<>(new String[]{"USER", "ADMIN"});
            UiLabels.install(typeCb);
            typeCb.setSelectedItem(target.getAccountType());
            JComboBox<String> statusCb = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
            UiLabels.install(statusCb);
            statusCb.setSelectedItem(target.getAccountStatus());

            JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
            form.add(new JLabel("Email:"));
            form.add(emailTf);
            form.add(new JLabel("First Name:"));
            form.add(firstTf);
            form.add(new JLabel("Last Name:"));
            form.add(lastTf);
            form.add(new JLabel("Account Type:"));
            form.add(typeCb);
            form.add(new JLabel("Account Status:"));
            form.add(statusCb);

            int choice = JOptionPane.showConfirmDialog(this, form,
                    "Edit User: " + target.getUsername(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            adminDAO.updateUserDetails(
                    userId,
                    emailTf.getText(),
                    firstTf.getText(),
                    lastTf.getText(),
                    String.valueOf(typeCb.getSelectedItem()),
                    String.valueOf(statusCb.getSelectedItem())
            );
            notifyChanged();
            loadUsersAsync(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "User account updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleSelectedUserStatus() {
        Integer userId = getSelectedUserId();
        if (userId == null) {
            return;
        }
        int row = userTable.getSelectedRow();
        String username = String.valueOf(userTableModel.getValueAt(row, 1));
        String currentStatus = String.valueOf(userTableModel.getValueAt(row, 5));
        String nextStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";

        int confirm = JOptionPane.showConfirmDialog(this,
                "Change account status of '" + username + "' from " + currentStatus + " to " + nextStatus + "?",
                "Confirm Status Change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminDAO.setUserStatus(userId, nextStatus);
            notifyChanged();
            loadUsersAsync(searchField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Action Blocked", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteSelectedUser() {
        Integer userId = getSelectedUserId();
        if (userId == null) {
            return;
        }
        int row = userTable.getSelectedRow();
        String username = String.valueOf(userTableModel.getValueAt(row, 1));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently delete user '" + username + "' (ID " + userId + ")?\n"
                + "All associated profile, settings, accounts, budgets, and transactions will also be removed.",
                "Confirm Delete User", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminDAO.deleteUser(userId);
            notifyChanged();
            loadUsersAsync(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "User deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Delete Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void notifyChanged() {
        if (onDataChangedCallback != null) {
            onDataChangedCallback.run();
        }
    }
}
