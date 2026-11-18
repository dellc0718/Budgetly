package budgetly.gui;

import budgetly.model.User;
import budgetly.service.AuthService;
import budgetly.service.BudgetService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;

/**
 * Login and Registration window for BUDGETLY.
 * Uses a CardLayout to switch between the Login form and the Registration form.
 */
public class LoginFrame extends JFrame {

    public static final Color BRAND_GREEN = new Color(34, 139, 74);
    public static final Color BRAND_DARK = new Color(20, 48, 42);
    public static final Color BG_LIGHT = new Color(245, 248, 246);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);

    private final AuthService authService;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Login inputs
    private final JTextField loginUsernameField = new JTextField();
    private final JPasswordField loginPasswordField = new JPasswordField();

    // Registration inputs
    private final JTextField regFullNameField = new JTextField();
    private final JTextField regUsernameField = new JTextField();
    private final JPasswordField regPasswordField = new JPasswordField();
    private final JPasswordField regConfirmField = new JPasswordField();

    public LoginFrame(AuthService authService) {
        this.authService = authService;
        setTitle("BUDGETLY - Personal Budget Tracker");
        setSize(480, 650);
        setMinimumSize(new Dimension(440, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        applyWindowIcon(this);

        cardPanel.setBackground(BG_LIGHT);
        cardPanel.add(buildLoginPanel(), "LOGIN");
        cardPanel.add(buildRegisterPanel(), "REGISTER");
        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel buildLoginPanel() {
        JPanel wrapper = createFormWrapper();
        wrapper.add(createLogoHeader("Welcome to BUDGETLY", "Sign in to manage your personal budget"));
        wrapper.add(Box.createVerticalStrut(18));

        wrapper.add(createLabeledField("Username", loginUsernameField));
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(createLabeledField("Password", loginPasswordField));
        wrapper.add(Box.createVerticalStrut(20));

        JButton loginButton = createPrimaryButton("Sign In");
        loginButton.addActionListener(e -> handleLogin());
        loginPasswordField.addActionListener(e -> handleLogin());

        JButton goRegisterButton = createSecondaryButton("Create a New Account");
        goRegisterButton.addActionListener(e -> {
            clearRegistrationFields();
            cardLayout.show(cardPanel, "REGISTER");
        });

        wrapper.add(loginButton);
        wrapper.add(Box.createVerticalStrut(10));
        wrapper.add(goRegisterButton);
        return wrapCentered(wrapper);
    }

    private JPanel buildRegisterPanel() {
        JPanel wrapper = createFormWrapper();
        wrapper.add(createLogoHeader("Create Regular Account", "Register to track your income, expenses & budget"));
        wrapper.add(Box.createVerticalStrut(14));

        wrapper.add(createLabeledField("Full Name", regFullNameField));
        wrapper.add(Box.createVerticalStrut(10));
        wrapper.add(createLabeledField("Username (3-20 letters, numbers or _)", regUsernameField));
        wrapper.add(Box.createVerticalStrut(10));
        wrapper.add(createLabeledField("Password (at least 6 characters)", regPasswordField));
        wrapper.add(Box.createVerticalStrut(10));
        wrapper.add(createLabeledField("Confirm Password", regConfirmField));
        wrapper.add(Box.createVerticalStrut(18));

        JButton registerButton = createPrimaryButton("Register Account");
        registerButton.addActionListener(e -> handleRegister());

        JButton backButton = createSecondaryButton("Back to Sign In");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "LOGIN"));

        wrapper.add(registerButton);
        wrapper.add(Box.createVerticalStrut(8));
        wrapper.add(backButton);
        return wrapCentered(wrapper);
    }

    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both your username and password.",
                    "Validation Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = authService.login(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password. Please try again.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Boolean.TRUE.equals(user.isActive())) {
                JOptionPane.showMessageDialog(this,
                        "Your account has been deactivated by an administrator.",
                        "Account Inactive", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Polymorphism: user.canManageUsers() decides which dashboard opens
            if (user.canManageUsers()) {
                new AdminDashboard(user, authService).setVisible(true);
            } else {
                new MainFrame(user, authService, new BudgetService()).setVisible(true);
            }
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error during login: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String fullName = regFullNameField.getText();
        String username = regUsernameField.getText();
        String password = new String(regPasswordField.getPassword());
        String confirm = new String(regConfirmField.getPassword());

        try {
            String error = authService.register(fullName, username, password, confirm);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error,
                        "Registration Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Account registered successfully! You can now sign in.",
                    "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
            loginUsernameField.setText(username.trim());
            loginPasswordField.setText("");
            clearRegistrationFields();
            cardLayout.show(cardPanel, "LOGIN");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error during registration: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearRegistrationFields() {
        regFullNameField.setText("");
        regUsernameField.setText("");
        regPasswordField.setText("");
        regConfirmField.setText("");
    }

    // ---------------- Shared UI & Logo Helpers ----------------

    private JPanel createFormWrapper() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 225, 218), 1),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)));
        return card;
    }

    private JPanel wrapCentered(JPanel card) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_LIGHT);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JPanel createLogoHeader(String titleText, String subtitleText) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoLabel = new JLabel();
        ImageIcon icon = loadLogoIcon(110, 110);
        if (icon != null) {
            logoLabel.setIcon(icon);
        }
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(BRAND_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(subtitleText, SwingConstants.CENTER);
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(new Color(95, 110, 102));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logoLabel);
        header.add(Box.createVerticalStrut(8));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JPanel createLabeledField(String labelText, JComponent field) {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel label = new JLabel(labelText);
        label.setFont(LABEL_FONT);
        label.setForeground(BRAND_DARK);

        field.setFont(INPUT_FONT);
        field.setPreferredSize(new Dimension(280, 34));
        panel.add(label);
        panel.add(field);
        return panel;
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(BRAND_GREEN);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 108, 56), 1),
                BorderFactory.createEmptyBorder(9, 18, 9, 18)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(232, 240, 235));
        btn.setForeground(BRAND_DARK);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 198, 188), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return btn;
    }

    /**
     * Loads and scales the official BUDGETLY logo from resources/logo.png.
     */
    public static ImageIcon loadLogoIcon(int width, int height) {
        Image image = loadRawLogoImage();
        if (image == null) {
            return null;
        }
        return new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public static void applyWindowIcon(JFrame frame) {
        Image image = loadRawLogoImage();
        if (image != null) {
            frame.setIconImage(image);
        }
    }

    private static Image loadRawLogoImage() {
        String[] candidatePaths = {
                "resources/logo.png",
                "src/budgetly/resources/logo.png",
                "Budgetly/resources/logo.png"
        };
        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath()).getImage();
            }
        }
        URL resource = LoginFrame.class.getResource("/budgetly/resources/logo.png");
        if (resource == null) {
            resource = LoginFrame.class.getResource("/logo.png");
        }
        return resource != null ? new ImageIcon(resource).getImage() : null;
    }
}
