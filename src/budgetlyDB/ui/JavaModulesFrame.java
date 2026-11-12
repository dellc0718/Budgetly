package budgetlyDB.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Opens the project Markdown guide in the operating system's default viewer. */
public class JavaModulesFrame extends JFrame {
    public JavaModulesFrame() {
        super("Budgetly Java Modules");
        BrandTheme.applyAppIcons(this);
        setSize(540, 190);
        setMinimumSize(new Dimension(460, 170));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        add(createModulesBrowserPanel());
    }

    public static File findDocumentation() {
        File current = new File(System.getProperty("user.dir", ".")).getAbsoluteFile();
        for (int i = 0; current != null && i < 5; i++, current = current.getParentFile()) {
            File guide = new File(current, "docs/JAVA_MODULES.md");
            if (guide.isFile()) return guide;
        }
        return new File("docs/JAVA_MODULES.md").getAbsoluteFile();
    }

    public static void openDocumentation() {
        File guide = findDocumentation();
        if (!guide.isFile()) {
            JOptionPane.showMessageDialog(null, "Java Modules guide was not found at: " + guide,
                    "Documentation unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("No document viewer is available on this computer.");
            }
            Desktop.getDesktop().open(guide);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Open this file in a Markdown viewer:\n" + guide
                    + "\n\n" + ex.getMessage(), "Open Java Modules", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static JPanel createModulesBrowserPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BrandTheme.getCardBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel message = new JLabel("Modules 1–8 are documented in docs/JAVA_MODULES.md");
        message.setFont(BrandTheme.FONT_SECTION);
        message.setForeground(BrandTheme.getTextPrimary());
        JButton open = BrandTheme.createPrimaryButton("Open Java Modules Guide");
        open.addActionListener(e -> openDocumentation());
        panel.add(message, BorderLayout.NORTH);
        panel.add(open, BorderLayout.CENTER);
        return panel;
    }
}
