package budgetlyDB.ui;

import budgetlyDB.AuthenticationService;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Regular user account registration window using Java Swing (JFrame).
 */
public class SignupFrame extends JFrame {

    private final AuthenticationService authService = new AuthenticationService();

    private JTextField usernameField;
    private JTextField emailField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton createAccountButton;
    private JButton backButton;
    private JLabel statusLabel;

    public SignupFrame() {
        super("Budgetly - Create Account");
        BrandTheme.applyAppIcons(this);
        setSize(920, 640);
        setMinimumSize(new Dimension(820, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createSideBrandPanel(), BorderLayout.WEST);
        add(createSignupFormPanel(), BorderLayout.CENTER);
        getRootPane().setDefaultButton(createAccountButton);
    }

    private JPanel createSideBrandPanel() {
        JPanel side = new JPanel();
        side.setBackground(BrandTheme.SIDEBAR_GREEN);
        side.setPreferredSize(new Dimension(320, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(32, 26, 26, 26));

        ImageIcon badge = BrandTheme.getMascotBadgeIcon(110);
        JLabel iconLbl = badge != null ? new JLabel(badge) : new JLabel("Budgetly");
        iconLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel("Join Budgetly", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='color:#D8E8DF;line-height:1.45;'>"
                + "<p>Create your personal account to start managing income, expenses, and monthly limits.</p><br>"
                + "<p><b>Security &amp; Privacy:</b><br>"
                + "• Passwords hashed with PBKDF2<br>"
                + "• Unique cryptographic random salt<br>"
                + "• Separate personal financial data<br>"
                + "• Saved profile &amp; currency settings</p>"
                + "</div></html>");
        desc.setFont(BrandTheme.FONT_SMALL);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        side.add(Box.createVerticalGlue());
        side.add(iconLbl);
        side.add(Box.createVerticalStrut(18));
        side.add(title);
        side.add(Box.createVerticalStrut(16));
        side.add(desc);
        side.add(Box.createVerticalGlue());

        return side;
    }

    private JPanel createSignupFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BrandTheme.CONTENT_BG);
        outer.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BrandTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(26, 28, 26, 28)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel heading = new JLabel("Create Your Account");
        heading.setFont(BrandTheme.FONT_TITLE);
        heading.setForeground(BrandTheme.TEXT_DARK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(heading, gbc);

        JLabel sub = new JLabel("All fields are required. Passwords must be at least 6 characters.");
        sub.setFont(BrandTheme.FONT_SUBTITLE);
        sub.setForeground(BrandTheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 18, 0);
        card.add(sub, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 4, 10);
        card.add(fieldLabel("First Name"), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 4, 0);
        card.add(fieldLabel("Last Name"), gbc);

        firstNameField = styledTextField();
        lastNameField = styledTextField();
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 12, 10);
        card.add(firstNameField, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 12, 0);
        card.add(lastNameField, gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Username"), gbc);

        usernameField = styledTextField();
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(usernameField, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(fieldLabel("Email Address"), gbc);

        emailField = styledTextField();
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(emailField, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 4, 10);
        card.add(fieldLabel("Password"), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 4, 0);
        card.add(fieldLabel("Confirm Password"), gbc);

        passwordField = styledPasswordField();
        confirmPasswordField = styledPasswordField();
        gbc.gridy = 9;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 12, 10);
        card.add(passwordField, gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 10, 12, 0);
        card.add(confirmPasswordField, gbc);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(BrandTheme.FONT_SMALL);
        statusLabel.setForeground(BrandTheme.DANGER_RED);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(statusLabel, gbc);

        createAccountButton = BrandTheme.createPrimaryButton("Create Account");
        createAccountButton.setPreferredSize(new Dimension(0, 42));
        createAccountButton.addActionListener(e -> performSignup());
        gbc.gridy = 11;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(createAccountButton, gbc);

        backButton = BrandTheme.createSecondaryButton("Already have an account? Back to Sign In");
        backButton.setPreferredSize(new Dimension(0, 38));
        backButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        gbc.gridy = 12;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(backButton, gbc);

        GridBagConstraints fill = new GridBagConstraints();
        fill.fill = GridBagConstraints.BOTH;
        fill.weightx = 1.0;
        fill.weighty = 1.0;
        outer.add(card, fill);
        return outer;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(BrandTheme.FONT_BODY_BOLD);
        lbl.setForeground(BrandTheme.TEXT_DARK);
        return lbl;
    }

    private JTextField styledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(BrandTheme.FONT_BODY);
        tf.setPreferredSize(new Dimension(0, 36));
        return tf;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(BrandTheme.FONT_BODY);
        pf.setPreferredSize(new Dimension(0, 36));
        return pf;
    }

    private void performSignup() {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String user = usernameField.getText().trim();
        String email = emailField.getText().trim();
        char[] pass = passwordField.getPassword();
        char[] confirm = confirmPasswordField.getPassword();

        setBusy(true, "Creating account in Apache Derby...");
        SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthenticationService.AuthResult doInBackground() {
                return authService.registerUser(user, email, first, last, pass, confirm);
            }

            @Override
            protected void done() {
                passwordField.setText("");
                confirmPasswordField.setText("");
                setBusy(false, null);
                try {
                    AuthenticationService.AuthResult result = get();
                    if (result.isSuccess()) {
                        JOptionPane.showMessageDialog(SignupFrame.this,
                                result.getMessage(),
                                "Account Created",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        new LoginFrame().setVisible(true);
                    } else {
                        statusLabel.setForeground(BrandTheme.DANGER_RED);
                        statusLabel.setText(result.getMessage());
                    }
                } catch (Exception ex) {
                    statusLabel.setForeground(BrandTheme.DANGER_RED);
                    statusLabel.setText("Registration error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void setBusy(boolean busy, String message) {
        createAccountButton.setEnabled(!busy);
        backButton.setEnabled(!busy);
        firstNameField.setEnabled(!busy);
        lastNameField.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        confirmPasswordField.setEnabled(!busy);
        if (message != null) {
            statusLabel.setForeground(BrandTheme.EMERALD);
            statusLabel.setText(message);
        }
    }
}
