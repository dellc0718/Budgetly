package budgetly.model;

/**
 * Shared interface for application records (users and transactions)
 * that can be searched in tables and summarized in dialogs.
 */
public interface Manageable {

    /**
     * @param keyword search text in lower-case
     * @return true when this record matches the search keyword
     */
    boolean matches(String keyword);

    /**
     * @return short one-line description of this record for dialogs
     */
    String getSummary();
}
