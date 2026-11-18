-- =============================================================
-- BUDGETLY - Personal Budget Tracker
-- Apache Derby (Java DB) schema
--
-- Database URL : jdbc:derby://localhost:1527/BudgetlyDB;create=true
-- DB user/pass : budgetly / budgetly   (tables live in schema BUDGETLY)
--
-- The application runs this script automatically the first time it
-- connects (see DatabaseConnection.initializeDatabase()).
-- You can also run it manually with Derby's "ij" tool or NetBeans.
--
-- The first administrator is NOT inserted here because passwords
-- must be hashed with a random salt. The application asks for the
-- administrator account on first start-up (Initial Admin Setup).
-- =============================================================

CREATE TABLE users (
    user_id       INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    full_name     VARCHAR(100) NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100),
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(10)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE transactions (
    transaction_id   INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    user_id          INTEGER       NOT NULL,
    description      VARCHAR(150)  NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    transaction_type VARCHAR(10)   NOT NULL,
    transaction_date DATE          NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (transaction_type IN ('INCOME', 'EXPENSE'))
);

CREATE TABLE budgets (
    budget_id     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    user_id       INTEGER       NOT NULL,
    monthly_limit DECIMAL(12,2) NOT NULL,
    budget_month  INTEGER       NOT NULL,
    budget_year   INTEGER       NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (budget_id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_budgets_user_month UNIQUE (user_id, budget_month, budget_year),
    CONSTRAINT ck_budgets_limit CHECK (monthly_limit > 0),
    CONSTRAINT ck_budgets_month CHECK (budget_month BETWEEN 1 AND 12)
);

CREATE INDEX idx_transactions_user ON transactions (user_id);
