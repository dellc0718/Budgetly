package budgetly;

import budgetly.database.DatabaseConnection;
import budgetly.gui.LoginFrame;
import budgetly.service.AuthService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of BUDGETLY - Personal Budget Tracker.
 * Initializes the Apache Derby database, ensures the default administrator
 * account exists, and opens the Swing LoginFrame on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();
            try {
                DatabaseConnection.initializeDatabase();
                AuthService authService = new AuthService();
                authService.initializeDefaultAdmin();
                new LoginFrame(authService).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize BUDGETLY database:\n" + ex.getMessage()
                                + "\n\nMake sure Apache Derby (derbyclient.jar / derbynet.jar) is on the classpath.",
                        "Database Startup Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default Metal look and feel
        }
    }
}
