from copy import deepcopy
from pathlib import Path
from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.text.paragraph import Paragraph

template = Path(r"C:\Users\wende\Downloads\CashTrack - Budget Tracker.docx")
out = Path(r"C:\Users\wende\Downloads\CashTrack - Budget Tracker - Complete.docx")
project = Path(r"C:\Users\wende\Downloads\git_v182_parity\Budgetly")
doc = Document(template)

module_content = {
1: (
    "The programs apply Module 1 by using Java's JDK, packages, external Derby libraries, and a main method to start BUDGETLY. The startup code initializes the database and opens the sign-in screen.",
    "public static void main(String[] args) {\n    SwingUtilities.invokeLater(() -> {\n        DatabaseConnection.initializeDatabase();\n        AuthService authService = new AuthService();\n        new LoginFrame(authService).setVisible(true);\n    });\n}"
),
2: (
    "The programs apply Module 2 by reading text entered in the transaction forms, converting money to BigDecimal, and calculating totals and balances accurately. The dashboard then displays those values to the user.",
    "BigDecimal total = BigDecimal.ZERO;\nfor (Transaction tx : transactions) {\n    if (tx.getType() == type) {\n        total = total.add(tx.getAmount());\n    }\n}"
),
3: (
    "The programs apply Module 3 by representing a monthly spending plan as a Budget object. Its private fields keep its data organized, while its constructor and methods initialize and calculate budget information.",
    "public class Budget {\n    private Integer budgetId;\n    private int userId;\n    private BigDecimal monthlyLimit;\n\n    public Budget(Integer budgetId, int userId,\n                  BigDecimal monthlyLimit, int month, int year) {\n        this.budgetId = budgetId;\n        this.userId = userId;\n        this.monthlyLimit = monthlyLimit;\n    }\n}"
),
4: (
    "The programs apply Module 4 through inheritance and polymorphism. Admin and RegularUser share the User base class, while Income and Expense share Transaction; each type can provide its own role or transaction behavior.",
    "public class Admin extends User {\n    @Override\n    public UserRole getRole() {\n        return UserRole.ADMIN;\n    }\n\n    @Override\n    public boolean canManageUsers() {\n        return true;\n    }\n}"
),
5: (
    "The programs apply Module 5 by checking inputs and handling database errors. This validation prevents incomplete or invalid account details from being saved and gives the user a clear message.",
    "if (fullName.trim().isEmpty() || username.trim().isEmpty()) {\n    return \"Full name and username are required.\";\n}\nif (!username.trim().matches(USERNAME_PATTERN)) {\n    return \"Username must be 3-20 characters.\";\n}"
),
6: (
    "The programs apply Module 6 by keeping transactions in lists so they can be searched and sorted for the Income, Expenses, and Transaction History tables. Generics let the same search logic work with different record types.",
    "public static <T extends Manageable> List<T> search(\n        List<T> items, String keyword) {\n    List<T> matches = new ArrayList<>();\n    for (T item : items) {\n        if (item.matches(keyword)) matches.add(item);\n    }\n    return matches;\n}"
),
7: (
    "The programs apply Module 7 by building the screens with Swing and responding to clicks. SwingWorker loads database records in the background, keeping the interface responsive while data is refreshed.",
    "new SwingWorker<List<Transaction>, Void>() {\n    @Override\n    protected List<Transaction> doInBackground() throws Exception {\n        return budgetService.getUserTransactions(currentUser.getUserId());\n    }\n    @Override\n    protected void done() {\n        allTransactions = get();\n        updateDashboardSection();\n    }\n}.execute();"
),
8: (
    "The programs apply Module 8 by connecting Java to Apache Derby through JDBC. A PreparedStatement saves transaction values safely, and the user ID keeps each record linked to its owner.",
    "String sql = \"INSERT INTO transactions (user_id, description, amount) VALUES (?, ?, ?)\";\ntry (Connection conn = DatabaseConnection.getConnection();\n     PreparedStatement ps = conn.prepareStatement(sql)) {\n    ps.setInt(1, t.getUserId());\n    ps.setString(2, t.getDescription());\n    ps.setBigDecimal(3, t.getAmount());\n    ps.executeUpdate();\n}"
),
}

def replace_paragraph(p, text):
    # Retain the paragraph and its first run's formatting.
    if p.runs:
        first = p.runs[0]
        first.text = text
        for run in p.runs[1:]:
            run.text = ""
    else:
        p.add_run(text)

for i, p in enumerate(doc.paragraphs):
    if p.text.strip() == "The programs ...":
        heading = next(x for x in reversed(doc.paragraphs[:i]) if x.text.strip().startswith("Module "))
        n = int(heading.text.strip().split()[-1])
        explanation, snippet = module_content[n]
        replace_paragraph(p, "\t" + explanation)
        target = doc.paragraphs[i+2]
        # Insert a code paragraph directly after the existing snippet label.
        new_p = OxmlElement("w:p")
        target._p.addnext(new_p)
        cp = Paragraph(new_p, target._parent)
        cp.paragraph_format.left_indent = Inches(0.25)
        cp.paragraph_format.space_after = Pt(5)
        cp.paragraph_format.line_spacing = 1.0
        r = cp.add_run(snippet)
        r.font.name = "Consolas"
        r.font.size = Pt(9)

replace_paragraph(doc.paragraphs[314], "\tBUDGETLY is a Java desktop application that helps users manage income, expenses, monthly budgets, and transaction history in one place. Its dashboard summarizes income, expenses, and balance, while search, sorting, and budget alerts help users understand their spending. The project uses Java Swing for its screens and Apache Derby with JDBC to save data. It also demonstrates the Java concepts in Modules 1-8, including object-oriented programming, collections, exception handling, and background work. The application serves as a practical tool for personal financial tracking and as a learning example of how these programming topics work together.")

# Populate the existing user-manual placeholder space, keeping the following section in place.
manual_header = doc.paragraphs[160]
screens = [
    ("Sign-in screen", "Enter your username and password to access the account. New users can choose Create a New Account.", "codex-clipboard-cd38fe83-9236-4465-b6dd-2c766a55073a.png"),
    ("Navigation menu", "The sidebar opens Dashboard, Income, Expenses, Budget, Transaction History, and Settings & Docs. Logout ends the current session.", "codex-clipboard-4f49130d-d1c0-4580-a865-db4a5bb37aa2.png"),
    ("Dashboard", "The dashboard summarizes total income, total expenses, current balance, and transaction count. Recent activity appears in the table.", "codex-clipboard-b61ce495-f123-4deb-90f9-883a8fd89816.png"),
    ("Income management", "Enter income details, then add or update a record. Search and sorting help locate saved income.", "codex-clipboard-7898da01-082e-4131-bad3-0dcb40d49cd5.png"),
    ("Expense management", "Enter expense details and save them. The table supports searching and sorting expense records.", "codex-clipboard-756a32e0-d9b8-46c9-8abf-27e3262bbc4a.png"),
    ("Monthly budget management", "Choose a month and year, set a spending limit, and review monthly expenses and the remaining budget.", "codex-clipboard-e89806dc-e03f-4314-bdcb-a9e02d319e56.png"),
    ("Transaction history", "Review income and expenses together. Search, filter by type, and sort records by date or amount.", "codex-clipboard-235d5b72-a30a-4d3e-a31c-98a197f31597.png"),
    ("Profile settings", "Review account details, update personal information, or change the password using the current password.", "codex-clipboard-57aa57ec-3aaf-45af-9af2-00ae2266d345.png"),
    ("User manual", "The Documentation & Learning Guide tab provides the in-app user manual and Java Modules 1-8 implementation guide.", "codex-clipboard-a7dc1307-f95e-48d8-9e0a-804cf74ac4b9.png"),
    ("Monthly budget monitor", "The monitor shows the selected month's limit, spending, and remaining amount. A prompt appears when no limit is set.", "codex-clipboard-c20eca8f-d8db-4c51-b01f-7fefbfc82106.png"),
]

# Place captions and images before the next report section, using the blank paragraphs already reserved.
insert_after = manual_header
for title, description, image_name in screens:
    cap = OxmlElement("w:p")
    insert_after._p.addnext(cap)
    cp = Paragraph(cap, insert_after._parent)
    cp.paragraph_format.keep_with_next = True
    cr = cp.add_run(title + ". ")
    cr.bold = True
    cr.font.size = Pt(10)
    cr2 = cp.add_run(description)
    cr2.font.size = Pt(10)
    pic_p = OxmlElement("w:p")
    cap.addnext(pic_p)
    pp = Paragraph(pic_p, cp._parent)
    pp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pic = project.parent / "AppData" / "Local" / "Temp" / image_name
    # Screenshots are in the user's Windows temp folder.
    pic = Path(r"C:\Users\wende\AppData\Local\Temp") / image_name
    # Fit both wide application views and narrow sign-in/sidebar captures to the template page.
    from PIL import Image
    with Image.open(pic) as source_image:
        aspect = source_image.height / source_image.width
    max_width, max_height = 6.25, 5.1
    width = min(max_width, max_height / aspect)
    pp.add_run().add_picture(str(pic), width=Inches(width))
    insert_after = pp

# Populate the existing file listing area with the main source files and a compact source excerpt per file.
files_start = doc.paragraphs[287]
file_entries = [
    ("src/budgetly/Main.java", "Starts BUDGETLY, initializes the database, and opens sign-in.", "DatabaseConnection.initializeDatabase();\nnew LoginFrame(authService).setVisible(true);"),
    ("src/budgetly/gui/LoginFrame.java", "Provides sign-in and account registration screens.", "User user = authService.login(username, password);"),
    ("src/budgetly/gui/MainFrame.java", "Displays the regular-user dashboard and budget tools.", "allTransactions = get();\nupdateDashboardSection();"),
    ("src/budgetly/gui/AdminDashboard.java", "Provides administrator account and statistics management.", "allUsers = loadedUsers;\npopulateUserTable();"),
    ("src/budgetly/service/AuthService.java", "Validates account data and handles authentication.", "if (!password.equals(confirmPassword)) {\n    return \"Password and confirm password do not match.\";\n}"),
    ("src/budgetly/service/BudgetService.java", "Validates, calculates, searches, and sorts financial records.", "BigDecimal balance = calculateBalance(transactions);"),
    ("src/budgetly/dao/UserDAO.java", "Reads and writes account records in the database.", "PreparedStatement ps = conn.prepareStatement(sql);"),
    ("src/budgetly/dao/TransactionDAO.java", "Stores and retrieves income and expense transactions.", "ps.setBigDecimal(3, t.getAmount());\nps.executeUpdate();"),
    ("src/budgetly/dao/BudgetDAO.java", "Stores monthly spending limits.", "budgetDAO.insert(new Budget(null, userId, limit, month, year));"),
    ("src/budgetly/database/DatabaseConnection.java", "Creates JDBC connections to the Derby database.", "return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);"),
    ("src/budgetly/model/User.java; Admin.java; RegularUser.java", "Model account data and its role-specific behavior.", "public abstract class User implements Manageable { ... }"),
    ("src/budgetly/model/Transaction.java; Income.java; Expense.java", "Model income and expense records.", "public abstract class Transaction implements Manageable { ... }"),
    ("src/budgetly/model/Budget.java", "Models a user's monthly spending limit.", "return monthlyLimit.subtract(spent);"),
    ("src/budgetly/model/Manageable.java; UserRole.java; TransactionType.java", "Define shared search behavior and account/transaction types.", "boolean matches(String keyword);"),
    ("database/schema.sql", "Defines the database tables and their constraints.", "CREATE TABLE transactions (...);"),
    ("lib/derby*.jar", "Provides the Apache Derby database and JDBC libraries.", "JDBC driver used by DriverManager.getConnection(...)."),
]
anchor = files_start
for filename, description, code in file_entries:
    for label, value in [("File Name: ", filename), ("File Description: ", description), ("Source Codes: ", code)]:
        el = OxmlElement("w:p")
        anchor._p.addnext(el)
        p = Paragraph(el, anchor._parent)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.0
        r = p.add_run(label)
        r.bold = True
        r.font.size = Pt(9)
        v = p.add_run(value)
        v.font.size = Pt(9)
        if label.startswith("Source Codes"):
            v.font.name = "Consolas"
        anchor = p

doc.save(out)
print(out)
