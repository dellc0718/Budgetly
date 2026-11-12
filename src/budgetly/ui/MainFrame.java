package budgetly.ui;

import budgetlyDB.AuthenticationService;
import budgetlyDB.DatabaseConnection;
import budgetlyDB.SessionManager;
import budgetlyDB.ui.AdminDashboard;
import budgetlyDB.ui.BrandTheme;
import budgetlyDB.ui.LoginFrame;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MainFrame extends JFrame implements ActionListener {

    private final AuthenticationService authService = new AuthenticationService();

    private JPanel sidebarPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel userBadgeLabel;

    private DashboardPanel dashboardPanel;
    private IncomePanel incomePanel;
    private ExpensesPanel expensesPanel;
    private MonthlyBudgetPanel monthlyBudgetPanel;
    private TransactionHistoryPanel transactionHistoryPanel;
    private AccountsPanel accountsPanel;
    private ProfilePanel profilePanel;

    public MainFrame() {
        super("Budgetly");
        SessionManager.requireLoggedIn();
        BrandTheme.applyAppIcons(this);

        setSize(1140, 720);
        setMinimumSize(new Dimension(960, 640));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        dashboardPanel = new DashboardPanel();
        incomePanel = new IncomePanel(this::refreshAllScreens);
        expensesPanel = new ExpensesPanel(this::refreshAllScreens);
        monthlyBudgetPanel = new MonthlyBudgetPanel(this::refreshAllScreens);
        transactionHistoryPanel = new TransactionHistoryPanel(this::refreshAllScreens);
        accountsPanel = new AccountsPanel(this::refreshAllScreens);
        profilePanel = new ProfilePanel(this::refreshAllScreens);

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(incomePanel, "Income");
        contentPanel.add(expensesPanel, "Expenses");
        contentPanel.add(monthlyBudgetPanel, "Monthly Budget");
        contentPanel.add(transactionHistoryPanel, "Transaction History");
        contentPanel.add(accountsPanel, "Accounts");
        contentPanel.add(profilePanel, "Profile");

        sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "Dashboard");
        setLocationRelativeTo(null);
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 18));
        sidebar.setBackground(BrandTheme.getSidebarBackground());
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // Top logo + mascot + user greeting
        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setOpaque(false);

        ImageIcon mascotIcon = BrandTheme.getMascotBadgeIcon(58);
        if (mascotIcon != null) {
            JLabel iconLabel = new JLabel(mascotIcon, SwingConstants.CENTER);
            iconLabel.setAlignmentX(CENTER_ALIGNMENT);
            brandBox.add(iconLabel);
            brandBox.add(Box.createVerticalStrut(6));
        }

        JLabel logoLabel = new JLabel("Budgetly", SwingConstants.CENTER);
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);
        brandBox.add(logoLabel);

        userBadgeLabel = new JLabel(formatUserBadge(), SwingConstants.CENTER);
        userBadgeLabel.setFont(BrandTheme.FONT_SMALL);
        userBadgeLabel.setForeground(BrandTheme.CREAM_ACCENT);
        userBadgeLabel.setAlignmentX(CENTER_ALIGNMENT);
        brandBox.add(Box.createVerticalStrut(4));
        brandBox.add(userBadgeLabel);

        sidebar.add(brandBox, BorderLayout.NORTH);

        // Navigation buttons
        String[] screenNames = {
            "Dashboard", "Income", "Expenses", "Monthly Budget",
            "Transaction History", "Accounts", "Profile"
        };

        int rows = screenNames.length + (SessionManager.isAdmin() ? 1 : 0);
        JPanel menuPanel = new JPanel(new GridLayout(rows, 1, 0, 8));
        menuPanel.setOpaque(false);

        for (String screenName : screenNames) {
            JButton button = new JButton(screenName);
            BrandTheme.styleSidebarButton(button, false);
            button.setActionCommand(screenName);
            button.addActionListener(this);
            menuPanel.add(button);
        }

        if (SessionManager.isAdmin()) {
            JButton adminBtn = new JButton("Admin Dashboard");
            BrandTheme.styleSidebarButton(adminBtn, true);
            adminBtn.setActionCommand("Admin Dashboard");
            adminBtn.addActionListener(this);
            menuPanel.add(adminBtn);
        }

        JPanel menuContainer = new JPanel(new BorderLayout());
        menuContainer.setOpaque(false);
        menuContainer.add(menuPanel, BorderLayout.NORTH);

        sidebar.add(menuContainer, BorderLayout.CENTER);

        // Footer buttons: Logout + Exit
        JPanel footerButtons = new JPanel(new GridLayout(2, 1, 0, 8));
        footerButtons.setOpaque(false);

        JButton logoutButton = new JButton("Logout");
        BrandTheme.styleSidebarButton(logoutButton, false);
        logoutButton.setActionCommand("Logout");
        logoutButton.addActionListener(this);

        JButton exitButton = new JButton("Exit");
        BrandTheme.styleSidebarButton(exitButton, false);
        exitButton.setActionCommand("Exit");
        exitButton.addActionListener(this);

        footerButtons.add(logoutButton);
        footerButtons.add(exitButton);
        sidebar.add(footerButtons, BorderLayout.SOUTH);

        return sidebar;
    }

    private String formatUserBadge() {
        String name = SessionManager.getDisplayName();
        String role = SessionManager.isAdmin() ? "Administrator" : "User";
        return name + " [" + role + "]";
    }

    public void refreshAllScreens() {
        sidebarPanel.setBackground(BrandTheme.getSidebarBackground());
        userBadgeLabel.setText(formatUserBadge());
        dashboardPanel.refreshData();
        incomePanel.refreshData();
        expensesPanel.refreshData();
        monthlyBudgetPanel.refreshData();
        transactionHistoryPanel.refreshData();
        accountsPanel.refreshData();
        profilePanel.refreshData();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("Exit".equals(cmd)) {
            confirmAndExit();
        } else if ("Logout".equals(cmd)) {
            confirmAndLogout();
        } else if ("Admin Dashboard".equals(cmd)) {
            if (!SessionManager.isAdmin()) {
                JOptionPane.showMessageDialog(this,
                        "Administrator privileges are required.",
                        "Access Denied", JOptionPane.ERROR_MESSAGE);
                return;
            }
            new AdminDashboard().setVisible(true);
        } else {
            refreshAllScreens();
            cardLayout.show(contentPanel, cmd);
        }
    }

    private void confirmAndLogout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of Budgetly?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
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

    private void confirmAndExit() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit Budgetly?",
                "Exit Budgetly",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            authService.logout();
            DatabaseConnection.shutdown();
            System.exit(0);
        }
    }
}
