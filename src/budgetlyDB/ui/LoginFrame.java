package budgetlyDB.ui;

import budgetly.ui.MainFrame;
import budgetlyDB.AuthenticationService;
import budgetlyDB.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Entry authentication window for Budgetly using Java Swing (JFrame).
 */
public class LoginFrame extends JFrame {

    private final AuthenticationService authService = new AuthenticationService();

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordBox;
    private JButton loginButton;
    private JButton signupButton;
    private JLabel statusLabel;
    private JLabel dbStatusPill;

    public LoginFrame() {
        super("Budgetly - Sign In");
        BrandTheme.applyAppIcons(this);
        setSize(880, 560);
        setMinimumSize(new Dimension(780, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createBrandPanel(), BorderLayout.WEST);
        add(createFormPanel(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(loginButton);
        refreshDbStatusAsync();
    }

    private JPanel createBrandPanel() {
        JPanel brandPanel = new JPanel();
        brandPanel.setBackground(BrandTheme.SIDEBAR_GREEN);
        brandPanel.setPreferredSize(new Dimension(350, 0));
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(BorderFactory.createEmptyBorder(34, 28, 28, 28));

        JPanel logoCard = new JPanel(new BorderLayout());
        logoCard.setBackground(Color.WHITE);
        logoCard.setMaximumSize(new Dimension(245, 245));
        logoCard.setPreferredSize(new Dimension(245, 245));
        logoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BrandTheme.CREAM_ACCENT, 3),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        logoCard.setAlignmentX(CENTER_ALIGNMENT);

        ImageIcon logoIcon = BrandTheme.getLogoImageIcon(215, 215);
        JLabel logoImgLabel = logoIcon != null
                ? new JLabel(logoIcon, SwingConstants.CENTER)
                : new JLabel("Budgetly", SwingConstants.CENTER);
        logoCard.add(logoImgLabel, BorderLayout.CENTER);

        JLabel titleLabel = new JLabel("Budgetly", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel taglineLabel = new JLabel("<html><div style='text-align:center;color:#D8E8DF;'>"
                + "Personal Finance, Accounts &amp;<br>Monthly Budget Tracking System</div></html>",
                SwingConstants.CENTER);
        taglineLabel.setFont(BrandTheme.FONT_BODY);
        taglineLabel.setAlignmentX(CENTER_ALIGNMENT);

        dbStatusPill = new JLabel("Checking Apache Derby...", SwingConstants.CENTER);
        dbStatusPill.setFont(BrandTheme.FONT_SMALL);
        dbStatusPill.setForeground(BrandTheme.CREAM_ACCENT);
        dbStatusPill.setAlignmentX(CENTER_ALIGNMENT);

        brandPanel.add(Box.createVerticalGlue());
        brandPanel.add(logoCard);
        brandPanel.add(Box.createVerticalStrut(20));
        brandPanel.add(titleLabel);
        brandPanel.add(Box.createVerticalStrut(8));
        brandPanel.add(taglineLabel);
        brandPanel.add(Box.createVerticalStrut(22));
        brandPanel.add(dbStatusPill);
        brandPanel.add(Box.createVerticalGlue());

        JLabel groupLabel = new JLabel("Group 5 | Budget Tracking System", SwingConstants.CENTER);
        groupLabel.setFont(BrandTheme.FONT_SMALL);
        groupLabel.setForeground(new Color(188, 208, 198));
        groupLabel.setAlignmentX(CENTER_ALIGNMENT);
        brandPanel.add(groupLabel);

        return brandPanel;
    }

    private JPanel createFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BrandTheme.CONTENT_BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BrandTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(30, 32, 30, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        JLabel heading = new JLabel("Welcome Back");
        heading.setFont(BrandTheme.FONT_TITLE);
        heading.setForeground(BrandTheme.TEXT_DARK);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(heading, gbc);

        JLabel sub = new JLabel("Sign in with your Budgetly username and password.");
        sub.setFont(BrandTheme.FONT_SUBTITLE);
        sub.setForeground(BrandTheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 22, 0);
        card.add(sub, gbc);

        JLabel userLbl = new JLabel("Username");
        userLbl.setFont(BrandTheme.FONT_BODY_BOLD);
        userLbl.setForeground(BrandTheme.TEXT_DARK);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        card.add(userLbl, gbc);

        usernameField = new JTextField(22);
        usernameField.setFont(BrandTheme.FONT_BODY);
        usernameField.setPreferredSize(new Dimension(0, 38));
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(usernameField, gbc);

        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(BrandTheme.FONT_BODY_BOLD);
        passLbl.setForeground(BrandTheme.TEXT_DARK);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 6, 0);
        card.add(passLbl, gbc);

        passwordField = new JPasswordField(22);
        passwordField.setFont(BrandTheme.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(0, 38));
        char defaultEcho = passwordField.getEchoChar();
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 8, 0);
        card.add(passwordField, gbc);

        showPasswordBox = new JCheckBox("Show password");
        showPasswordBox.setOpaque(false);
        showPasswordBox.setFont(BrandTheme.FONT_SMALL);
        showPasswordBox.setForeground(BrandTheme.TEXT_MUTED);
        showPasswordBox.addActionListener(e ->
                passwordField.setEchoChar(showPasswordBox.isSelected() ? (char) 0 : defaultEcho));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(showPasswordBox, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(BrandTheme.FONT_SMALL);
        statusLabel.setForeground(BrandTheme.DANGER_RED);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(statusLabel, gbc);

        loginButton = BrandTheme.createPrimaryButton("Sign In");
        loginButton.setPreferredSize(new Dimension(0, 42));
        loginButton.addActionListener(e -> performLogin());
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(loginButton, gbc);

        signupButton = BrandTheme.createSecondaryButton("Create New Account");
        signupButton.setPreferredSize(new Dimension(0, 40));
        signupButton.addActionListener(e -> openSignup());
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(signupButton, gbc);

        JLabel infoNote = new JLabel("<html><span style='color:#607069;'>"
                + "New users receive isolated personal accounts, profile settings, and starter Cash Wallet."
                + "</span></html>");
        infoNote.setFont(BrandTheme.FONT_SMALL);
        gbc.gridy = 10;
        gbc.insets = new Insets(4, 0, 0, 0);
        card.add(infoNote, gbc);

        GridBagConstraints outer = new GridBagConstraints();
        outer.fill = GridBagConstraints.BOTH;
        outer.weightx = 1.0;
        outer.weighty = 1.0;
        wrapper.add(card, outer);

        return wrapper;
    }

    private void refreshDbStatusAsync() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    boolean ok = get();
                    if (ok) {
                        dbStatusPill.setText("● Java DB (Apache Derby) Connected");
                        dbStatusPill.setForeground(new Color(180, 245, 200));
                    } else {
                        dbStatusPill.setText("● Database Connection Unavailable");
                        dbStatusPill.setForeground(new Color(255, 190, 190));
                    }
                } catch (Exception ex) {
                    dbStatusPill.setText("● Database Status Error");
                    dbStatusPill.setForeground(new Color(255, 190, 190));
                }
            }
        };
        worker.execute();
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (username.isEmpty() || password.length == 0) {
            statusLabel.setForeground(BrandTheme.DANGER_RED);
            statusLabel.setText("Please enter your username and password.");
            return;
        }

        setBusy(true, "Authenticating securely...");
        SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthenticationService.AuthResult doInBackground() {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                passwordField.setText("");
                setBusy(false, null);
                try {
                    AuthenticationService.AuthResult result = get();
                    if (result.isSuccess()) {
                        dispose();
                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                    } else {
                        statusLabel.setForeground(BrandTheme.DANGER_RED);
                        statusLabel.setText(result.getMessage());
                    }
                } catch (Exception ex) {
                    statusLabel.setForeground(BrandTheme.DANGER_RED);
                    statusLabel.setText("Login error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Unable to complete sign in: " + ex.getMessage(),
                            "Authentication Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openSignup() {
        dispose();
        SignupFrame signup = new SignupFrame();
        signup.setVisible(true);
    }

    private void setBusy(boolean busy, String message) {
        loginButton.setEnabled(!busy);
        signupButton.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        if (message != null) {
            statusLabel.setForeground(BrandTheme.EMERALD);
            statusLabel.setText(message);
        }
    }
}
