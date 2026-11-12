package budgetlyDB.ui;

import budgetlyDB.AuthenticationService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.User;
import budgetlyDB.model.UserProfile;
import budgetlyDB.model.UserSettings;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Standalone JFrame for editing the signed-in user's profile, preferences, and password.
 * Exposes a reusable ProfileSettingsContentPanel so MainFrame's Profile tab shares the exact same UI.
 */
public class ProfileSettingsFrame extends JFrame {

    public ProfileSettingsFrame() {
        this(null);
    }

    public ProfileSettingsFrame(Runnable onSavedCallback) {
        super("Budgetly - Profile & Settings");
        SessionManager.requireLoggedIn();
        BrandTheme.applyAppIcons(this);
        setSize(940, 700);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BrandTheme.SIDEBAR_GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        ImageIcon icon = BrandTheme.getMascotBadgeIcon(34);
        if (icon != null) {
            left.add(new JLabel(icon));
        }
        JLabel title = new JLabel("User Profile & Application Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        left.add(title);
        JButton closeBtn = BrandTheme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        header.add(left, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new ProfileSettingsContentPanel(onSavedCallback), BorderLayout.CENTER);
    }

    /**
     * Reusable panel for profile, settings, and password management.
     */
    public static class ProfileSettingsContentPanel extends JPanel {
        private final AuthenticationService authService = new AuthenticationService();
        private final Runnable onSavedCallback;

        private JTextField usernameField;
        private JTextField roleField;
        private JTextField emailField;
        private JTextField firstNameField;
        private JTextField lastNameField;
        private JTextField displayNameField;
        private JTextField contactField;
        private JTextField occupationField;
        private JTextField addressField;
        private JTextArea bioArea;
        private JComboBox<String> avatarCombo;

        private JComboBox<String> currencyCombo;
        private JComboBox<String> themeCombo;
        private JComboBox<String> notificationCombo;
        private JCheckBox budgetAlertsBox;
        private JCheckBox emailSummaryBox;
        private JSpinner alertPercentSpinner;

        private JPasswordField currentPassField;
        private JPasswordField newPassField;
        private JPasswordField confirmPassField;

        private JButton saveProfileButton;
        private JButton changePasswordButton;

        public ProfileSettingsContentPanel(Runnable onSavedCallback) {
            this.onSavedCallback = onSavedCallback;
            setLayout(new BorderLayout(0, 16));
            setBackground(BrandTheme.getContentBackground());
            setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

            JPanel cardsGrid = new JPanel(new GridLayout(1, 2, 18, 0));
            cardsGrid.setOpaque(false);
            cardsGrid.add(buildProfileCard());
            cardsGrid.add(buildSettingsAndPasswordColumn());

            JScrollPane scroll = new JScrollPane(cardsGrid);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BrandTheme.getContentBackground());
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            add(scroll, BorderLayout.CENTER);

            loadFromSession();
        }

        private JPanel buildProfileCard() {
            JPanel card = BrandTheme.createCardPanel();
            card.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridx = 0;

            JLabel title = new JLabel("Personal Profile Information");
            title.setFont(BrandTheme.FONT_SECTION);
            title.setForeground(BrandTheme.getTextPrimary());
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(0, 0, 12, 0);
            card.add(title, gbc);

            gbc.gridwidth = 1;
            usernameField = readOnlyField();
            roleField = readOnlyField();
            addPair(card, gbc, 1, "Username", usernameField, "Account Role", roleField);

            firstNameField = editableField();
            lastNameField = editableField();
            addPair(card, gbc, 3, "First Name", firstNameField, "Last Name", lastNameField);

            emailField = editableField();
            displayNameField = editableField();
            addPair(card, gbc, 5, "Email Address", emailField, "Display Name", displayNameField);

            contactField = editableField();
            occupationField = editableField();
            addPair(card, gbc, 7, "Contact Number", contactField, "Occupation", occupationField);

            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 9;
            gbc.insets = new Insets(4, 0, 4, 0);
            card.add(styledLabel("Address"), gbc);
            addressField = editableField();
            gbc.gridy = 10;
            gbc.insets = new Insets(0, 0, 10, 0);
            card.add(addressField, gbc);

            gbc.gridy = 11;
            gbc.insets = new Insets(0, 0, 4, 0);
            card.add(styledLabel("Profile Avatar Style"), gbc);
            avatarCombo = new JComboBox<>(new String[]{"default_croc", "emerald_badge", "classic_initials"});
            UiLabels.install(avatarCombo);
            avatarCombo.setFont(BrandTheme.FONT_BODY);
            gbc.gridy = 12;
            gbc.insets = new Insets(0, 0, 10, 0);
            card.add(avatarCombo, gbc);

            gbc.gridy = 13;
            gbc.insets = new Insets(0, 0, 4, 0);
            card.add(styledLabel("Short Bio / Notes"), gbc);
            bioArea = new JTextArea(3, 20);
            bioArea.setFont(BrandTheme.FONT_BODY);
            bioArea.setLineWrap(true);
            bioArea.setWrapStyleWord(true);
            JScrollPane bioScroll = new JScrollPane(bioArea);
            gbc.gridy = 14;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 14, 0);
            card.add(bioScroll, gbc);

            saveProfileButton = BrandTheme.createPrimaryButton("Save Profile & Preferences");
            saveProfileButton.addActionListener(e -> saveProfileAndSettings());
            gbc.gridy = 15;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 0, 0);
            card.add(saveProfileButton, gbc);

            return card;
        }

        private JPanel buildSettingsAndPasswordColumn() {
            JPanel col = new JPanel(new BorderLayout(0, 16));
            col.setOpaque(false);

            // Settings Card
            JPanel settingsCard = BrandTheme.createCardPanel();
            settingsCard.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridx = 0;

            JLabel sTitle = new JLabel("Application Preferences");
            sTitle.setFont(BrandTheme.FONT_SECTION);
            sTitle.setForeground(BrandTheme.getTextPrimary());
            gbc.gridy = 0;
            gbc.insets = new Insets(0, 0, 12, 0);
            settingsCard.add(sTitle, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(0, 0, 4, 0);
            settingsCard.add(styledLabel("Preferred Currency"), gbc);
            currencyCombo = new JComboBox<>(new String[]{"PHP", "USD", "EUR", "GBP", "JPY"});
            currencyCombo.setFont(BrandTheme.FONT_BODY);
            gbc.gridy = 2;
            gbc.insets = new Insets(0, 0, 10, 0);
            settingsCard.add(currencyCombo, gbc);

            gbc.gridy = 3;
            gbc.insets = new Insets(0, 0, 4, 0);
            settingsCard.add(styledLabel("Interface Theme"), gbc);
            themeCombo = new JComboBox<>(new String[]{"LIGHT", "FOREST", "DARK"});
            UiLabels.install(themeCombo);
            themeCombo.setFont(BrandTheme.FONT_BODY);
            gbc.gridy = 4;
            gbc.insets = new Insets(0, 0, 10, 0);
            settingsCard.add(themeCombo, gbc);

            gbc.gridy = 5;
            gbc.insets = new Insets(0, 0, 4, 0);
            settingsCard.add(styledLabel("Notification Mode"), gbc);
            notificationCombo = new JComboBox<>(new String[]{
                    "BUDGET_AND_SECURITY", "BUDGET_ONLY", "SECURITY_ONLY", "MINIMAL"
            });
            UiLabels.install(notificationCombo);
            notificationCombo.setFont(BrandTheme.FONT_BODY);
            gbc.gridy = 6;
            gbc.insets = new Insets(0, 0, 10, 0);
            settingsCard.add(notificationCombo, gbc);

            gbc.gridy = 7;
            gbc.insets = new Insets(0, 0, 4, 0);
            settingsCard.add(styledLabel("Budget Alert Threshold (%)"), gbc);
            alertPercentSpinner = new JSpinner(new SpinnerNumberModel(80, 10, 100, 5));
            alertPercentSpinner.setFont(BrandTheme.FONT_BODY);
            gbc.gridy = 8;
            gbc.insets = new Insets(0, 0, 10, 0);
            settingsCard.add(alertPercentSpinner, gbc);

            budgetAlertsBox = new JCheckBox("Show monthly budget warning alerts on Dashboard");
            budgetAlertsBox.setOpaque(false);
            budgetAlertsBox.setFont(BrandTheme.FONT_BODY);
            budgetAlertsBox.setForeground(BrandTheme.getTextPrimary());
            gbc.gridy = 9;
            gbc.insets = new Insets(0, 0, 6, 0);
            settingsCard.add(budgetAlertsBox, gbc);

            emailSummaryBox = new JCheckBox("Enable monthly summary notifications");
            emailSummaryBox.setOpaque(false);
            emailSummaryBox.setFont(BrandTheme.FONT_BODY);
            emailSummaryBox.setForeground(BrandTheme.getTextPrimary());
            gbc.gridy = 10;
            gbc.insets = new Insets(0, 0, 0, 0);
            settingsCard.add(emailSummaryBox, gbc);

            // Password Card
            JPanel passCard = BrandTheme.createCardPanel();
            passCard.setLayout(new GridBagLayout());
            GridBagConstraints pbc = new GridBagConstraints();
            pbc.fill = GridBagConstraints.HORIZONTAL;
            pbc.weightx = 1.0;
            pbc.gridx = 0;

            JLabel pTitle = new JLabel("Security & Password Change");
            pTitle.setFont(BrandTheme.FONT_SECTION);
            pTitle.setForeground(BrandTheme.getTextPrimary());
            pbc.gridy = 0;
            pbc.insets = new Insets(0, 0, 10, 0);
            passCard.add(pTitle, pbc);

            pbc.gridy = 1;
            pbc.insets = new Insets(0, 0, 4, 0);
            passCard.add(styledLabel("Current Password"), pbc);
            currentPassField = new JPasswordField();
            currentPassField.setPreferredSize(new Dimension(0, 34));
            pbc.gridy = 2;
            pbc.insets = new Insets(0, 0, 8, 0);
            passCard.add(currentPassField, pbc);

            pbc.gridy = 3;
            pbc.insets = new Insets(0, 0, 4, 0);
            passCard.add(styledLabel("New Password (min 6 characters)"), pbc);
            newPassField = new JPasswordField();
            newPassField.setPreferredSize(new Dimension(0, 34));
            pbc.gridy = 4;
            pbc.insets = new Insets(0, 0, 8, 0);
            passCard.add(newPassField, pbc);

            pbc.gridy = 5;
            pbc.insets = new Insets(0, 0, 4, 0);
            passCard.add(styledLabel("Confirm New Password"), pbc);
            confirmPassField = new JPasswordField();
            confirmPassField.setPreferredSize(new Dimension(0, 34));
            pbc.gridy = 6;
            pbc.insets = new Insets(0, 0, 12, 0);
            passCard.add(confirmPassField, pbc);

            changePasswordButton = BrandTheme.createPrimaryButton("Update Password");
            changePasswordButton.addActionListener(e -> performPasswordChange());
            pbc.gridy = 7;
            pbc.insets = new Insets(0, 0, 0, 0);
            passCard.add(changePasswordButton, pbc);

            col.add(settingsCard, BorderLayout.NORTH);
            col.add(passCard, BorderLayout.CENTER);
            return col;
        }

        private void addPair(JPanel panel, GridBagConstraints gbc, int row,
                             String lbl1, JTextField tf1, String lbl2, JTextField tf2) {
            gbc.gridy = row;
            gbc.gridx = 0;
            gbc.insets = new Insets(0, 0, 4, 8);
            panel.add(styledLabel(lbl1), gbc);
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 8, 4, 0);
            panel.add(styledLabel(lbl2), gbc);

            gbc.gridy = row + 1;
            gbc.gridx = 0;
            gbc.insets = new Insets(0, 0, 10, 8);
            panel.add(tf1, gbc);
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 8, 10, 0);
            panel.add(tf2, gbc);
        }

        private JLabel styledLabel(String text) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(BrandTheme.FONT_SMALL);
            lbl.setForeground(BrandTheme.getTextMuted());
            return lbl;
        }

        private JTextField editableField() {
            JTextField tf = new JTextField();
            tf.setFont(BrandTheme.FONT_BODY);
            tf.setPreferredSize(new Dimension(0, 34));
            return tf;
        }

        private JTextField readOnlyField() {
            JTextField tf = new JTextField();
            tf.setFont(BrandTheme.FONT_BODY);
            tf.setEditable(false);
            tf.setBackground(new Color(238, 242, 240));
            tf.setPreferredSize(new Dimension(0, 34));
            return tf;
        }

        public final void loadFromSession() {
            User user = SessionManager.getCurrentUser();
            UserProfile profile = SessionManager.getCurrentProfile();
            UserSettings settings = SessionManager.getCurrentSettings();
            if (user == null) {
                return;
            }
            usernameField.setText(user.getUsername());
            roleField.setText(UiLabels.label(user.getAccountType()) + " (" + UiLabels.label(user.getAccountStatus()) + ")");
            emailField.setText(user.getEmail());
            firstNameField.setText(user.getFirstName());
            lastNameField.setText(user.getLastName());

            if (profile != null) {
                displayNameField.setText(profile.getDisplayName());
                contactField.setText(profile.getContactNumber());
                occupationField.setText(profile.getOccupation());
                addressField.setText(profile.getAddress());
                bioArea.setText(profile.getBio());
                avatarCombo.setSelectedItem(profile.getProfilePicture());
            }

            if (settings != null) {
                currencyCombo.setSelectedItem(settings.getCurrencyPreference());
                themeCombo.setSelectedItem(settings.getTheme());
                notificationCombo.setSelectedItem(settings.getNotificationPreferences());
                budgetAlertsBox.setSelected(settings.isBudgetAlertsEnabled());
                emailSummaryBox.setSelected(settings.isEmailSummaryEnabled());
                alertPercentSpinner.setValue(settings.getDefaultBudgetAlertPercent());
            }
        }

        private void saveProfileAndSettings() {
            String email = emailField.getText().trim();
            String first = firstNameField.getText().trim();
            String last = lastNameField.getText().trim();

            UserProfile profile = new UserProfile();
            profile.setDisplayName(displayNameField.getText().trim());
            profile.setContactNumber(contactField.getText().trim());
            profile.setOccupation(occupationField.getText().trim());
            profile.setAddress(addressField.getText().trim());
            profile.setBio(bioArea.getText().trim());
            profile.setProfilePicture(String.valueOf(avatarCombo.getSelectedItem()));

            UserSettings settings = new UserSettings();
            settings.setCurrencyPreference(String.valueOf(currencyCombo.getSelectedItem()));
            settings.setTheme(String.valueOf(themeCombo.getSelectedItem()));
            settings.setNotificationPreferences(String.valueOf(notificationCombo.getSelectedItem()));
            settings.setBudgetAlertsEnabled(budgetAlertsBox.isSelected());
            settings.setEmailSummaryEnabled(emailSummaryBox.isSelected());
            settings.setDefaultBudgetAlertPercent((Integer) alertPercentSpinner.getValue());

            saveProfileButton.setEnabled(false);
            SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
                @Override
                protected AuthenticationService.AuthResult doInBackground() {
                    return authService.saveCurrentUserProfileAndSettings(email, first, last, profile, settings);
                }

                @Override
                protected void done() {
                    saveProfileButton.setEnabled(true);
                    try {
                        AuthenticationService.AuthResult res = get();
                        if (res.isSuccess()) {
                            loadFromSession();
                            if (onSavedCallback != null) {
                                onSavedCallback.run();
                            }
                            JOptionPane.showMessageDialog(ProfileSettingsContentPanel.this,
                                    res.getMessage(), "Saved", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ProfileSettingsContentPanel.this,
                                    res.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProfileSettingsContentPanel.this,
                                "Error saving profile: " + ex.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }

        private void performPasswordChange() {
            char[] cur = currentPassField.getPassword();
            char[] next = newPassField.getPassword();
            char[] conf = confirmPassField.getPassword();

            changePasswordButton.setEnabled(false);
            SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
                @Override
                protected AuthenticationService.AuthResult doInBackground() {
                    return authService.changePassword(cur, next, conf);
                }

                @Override
                protected void done() {
                    currentPassField.setText("");
                    newPassField.setText("");
                    confirmPassField.setText("");
                    changePasswordButton.setEnabled(true);
                    try {
                        AuthenticationService.AuthResult res = get();
                        JOptionPane.showMessageDialog(
                                ProfileSettingsContentPanel.this,
                                res.getMessage(),
                                res.isSuccess() ? "Password Changed" : "Password Error",
                                res.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                        );
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProfileSettingsContentPanel.this,
                                "Failed to change password: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        }
    }
}
