package budgetly;

import budgetlyDB.DatabaseConnection;
import budgetlyDB.DatabaseInitializer;
import budgetlyDB.ui.LoginFrame;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Main application entry point for Budgetly.
 * Initializes Apache Derby tables and default administrator account before presenting LoginFrame.
 */
public class Budgetly {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown, "Budgetly-DB-Shutdown"));

        try {
            DatabaseInitializer.initialize();
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Budgetly could not initialize the Apache Derby database.\n\nDetails: " + ex.getMessage(),
                    "Database Startup Error",
                    JOptionPane.ERROR_MESSAGE
            ));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
