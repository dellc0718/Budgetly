# MODULE 1 – Java Compiler, JDK, IDE and Development Environment

## 1. Module Title
**Module 1: Java Compiler, JDK, IDE and Development Environment**

## 2. Concepts Covered
- **Java Development Kit (JDK)**: The full software development kit containing the compiler (`javac`), runtime tools, and standard libraries (`java.sql`, `javax.swing`, `java.time`, `javax.crypto`).
- **Java Runtime Environment (JRE) & Java Virtual Machine (JVM)**: The runtime environment and virtual machine that load compiled `.class` bytecode files and execute them on the host operating system.
- **Java Compiler (`javac`)**: Translates human-readable `.java` source files into platform-independent `.class` bytecode files.
- **Project Structure & Packages**: Organizing classes into logical namespaces (`budgetly`, `budgetly.database`, `budgetly.model`, `budgetly.dao`, `budgetly.service`, `budgetly.gui`).
- **External Libraries (Classpath `-cp`)**: Linking the Apache Derby Client JDBC driver (`lib/derbyclient.jar`) at compile time and runtime.

---

## 3. How These Concepts Are Applied in BUDGETLY
BUDGETLY is organized into 6 clean packages across 20 Java files:
- `budgetly.Main` is the entry point (`public static void main(String[] args)`) that the JVM invokes when the program starts.
- Each `.java` file begins with a `package` declaration matching its folder path under `src/budgetly/`.
- The Apache Derby libraries in `lib/` (`derbyclient.jar`, `derby.jar`, `derbynet.jar`, `derbyshared.jar`, `derbytools.jar`) provide the JDBC client driver and network server used at runtime.

### Compiling and Running BUDGETLY with `javac` and `java`
From the `Budgetly` project directory:
```
javac -cp ".;lib/derbyclient.jar;lib/*" -d bin src/budgetly/Main.java src/budgetly/database/*.java src/budgetly/model/*.java src/budgetly/dao/*.java src/budgetly/service/*.java src/budgetly/gui/*.java

java -cp "bin;lib/derbyclient.jar;lib/*" budgetly.Main
```
*Note*: Because BUDGETLY uses multiple packages (`database`, `model`, `dao`, `service`, `gui`), passing all source directories (or using an IDE build configuration in NetBeans, IntelliJ IDEA, or Eclipse) compiles every `.java` file into its matching folder inside `bin/`.

---

## 4. Actual Source Code from BUDGETLY

### Source File: `src/budgetly/Main.java`
### Class & Method: `Main.main(String[] args)` and `Main.applyLookAndFeel()`

```java
package budgetly;

import budgetly.database.DatabaseConnection;
import budgetly.gui.LoginFrame;
import budgetly.service.AuthService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
```

---

## 5. Explanation of Important Lines of Code
- `package budgetly;`: Declares the root package for the entry point class.
- `import budgetly.database.DatabaseConnection;`: Imports classes from other packages in the project so `Main` can coordinate database startup and GUI creation.
- `public static void main(String[] args)`: The standard entry method called by the JVM when launching `java -cp "bin;lib/*" budgetly.Main`.
- `DatabaseConnection.initializeDatabase();`: Connects to Apache Derby using the JDBC driver on the classpath and runs `database/schema.sql` if tables do not exist yet.
- `authService.initializeDefaultAdmin();`: Ensures the initial administrator account exists on first launch.
- `new LoginFrame(authService).setVisible(true);`: Instantiates and displays the main login window.

## 6. How It Works Within the Application
1. The developer or student compiles the `.java` files using `javac` (or the IDE's Build command), producing `.class` bytecode files under `bin/budgetly/...`.
2. The JVM starts `budgetly.Main`, loads the Derby Client driver from `lib/derbyclient.jar`, prepares the database schema, and opens `LoginFrame` on the screen.
