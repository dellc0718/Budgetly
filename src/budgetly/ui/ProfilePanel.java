package budgetly.ui;

import budgetlyDB.ui.BrandTheme;
import budgetlyDB.ui.ProfileSettingsFrame;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Embedded Profile & Settings screen inside MainFrame, wrapping the shared ProfileSettingsContentPanel
 * and providing a pop-out option to open the standalone ProfileSettingsFrame (JFrame).
 */
public class ProfilePanel extends JPanel {

    private final ProfileSettingsFrame.ProfileSettingsContentPanel contentPanel;
    private JLabel headingLabel;

    public ProfilePanel(Runnable onSavedCallback) {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 10, 22));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        headingLabel = new JLabel("Profile & Settings");
        headingLabel.setFont(BrandTheme.FONT_TITLE);

        JButton popoutBtn = BrandTheme.createSecondaryButton("Open Standalone Settings Window");
        popoutBtn.addActionListener(e -> new ProfileSettingsFrame(() -> {
            refreshData();
            if (onSavedCallback != null) {
                onSavedCallback.run();
            }
        }).setVisible(true));

        top.add(headingLabel, BorderLayout.WEST);
        top.add(popoutBtn, BorderLayout.EAST);

        contentPanel = new ProfileSettingsFrame.ProfileSettingsContentPanel(onSavedCallback);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        add(top, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        applyTheme();
    }

    public final void applyTheme() {
        setBackground(BrandTheme.getContentBackground());
        headingLabel.setForeground(BrandTheme.getTextPrimary());
        contentPanel.setBackground(BrandTheme.getContentBackground());
    }

    public final void refreshData() {
        applyTheme();
        contentPanel.loadFromSession();
    }
}
