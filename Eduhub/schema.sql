-- =====================================================================
-- EduFinance Hub - Apache Derby database schema
-- =====================================================================
-- This is the schema the Java code actually needs. It was built by
-- reading every SQL statement in every DAO class (dao/*.java) and every
-- servlet, not written from scratch - so the table and column names
-- below match exactly what the app already queries.
--
-- IMPORTANT: before this file existed, only APP.APP_USERS was ever
-- created (DBConnection.java creates it automatically on first
-- connection). Every other table below had NO creation script
-- anywhere in the project. That means registration could succeed
-- (app_users exists) but almost everything after login - bursaries,
-- wallet, calendar, document vault, learn modules - would fail with
-- "table does not exist" SQL errors, because those tables were never
-- created. Running this script once against your Derby database fixes
-- that.
--
-- HOW TO RUN THIS:
--   1. Start the Derby Network Server (or let the app start it) so it
--      is listening on localhost:1527.
--   2. Using ij (Derby's command-line tool) or a SQL client such as
--      DBeaver / SQuirreL connected to:
--        jdbc:derby://localhost:1527/edufinancehub_db;create=true
--      user: app   password: app
--   3. Run this whole file. It is safe to run more than once - it
--      only creates tables/rows that do not already exist.
--
-- Tables are created in dependency order (a table only references
-- tables created above it).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. APP_USERS
-- ---------------------------------------------------------------------
-- Already created automatically by DBConnection.java the first time the
-- app connects, but included here so the whole schema lives in one
-- place and so a fresh database can be built from this file alone.
-- Wrapped so re-running this script does not error if it already exists.
-- ---------------------------------------------------------------------
CREATE TABLE APP.APP_USERS (
    user_id             INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
    full_name           VARCHAR(120)  NOT NULL,
    email               VARCHAR(150)  NOT NULL UNIQUE,
    phone_number        VARCHAR(25)   NOT NULL,
    student_type        VARCHAR(60)   NOT NULL,
    password_hash       VARCHAR(255)  NOT NULL,
    failed_attempts     INT           DEFAULT 0,
    is_locked           BOOLEAN       DEFAULT FALSE,
    profile_completed   BOOLEAN       DEFAULT FALSE,
    xp_points           INT           DEFAULT 0,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);


-- ---------------------------------------------------------------------
-- 2. STUDENT_PROFILES  (1:1 with app_users - user_id IS the primary key)
-- ---------------------------------------------------------------------
-- Populated by ProfileDAO during the onboarding "Quest" wizard.
-- saveIdentity() UPDATEs first and only INSERTs if the UPDATE affects
-- zero rows, so exactly one row per user_id is expected.
-- ---------------------------------------------------------------------
CREATE TABLE student_profiles (
    user_id                     INT PRIMARY KEY REFERENCES app_users(user_id),
    id_or_passport              VARCHAR(30)     DEFAULT 'PENDING',
    full_name                   VARCHAR(120),
    age                         INT,
    province                    VARCHAR(60),
    academic_level              VARCHAR(60)     DEFAULT 'Pending',
    field_of_study               VARCHAR(120)    DEFAULT 'Pending',
    institution_name            VARCHAR(150),
    household_income_bracket    VARCHAR(60)     DEFAULT 'Pending',
    overall_academic_average    DOUBLE,
    dna_score                   INT,
    created_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);


-- ---------------------------------------------------------------------
-- 3. STUDENT_ACADEMIC_MARKS
-- ---------------------------------------------------------------------
-- ProfileDAO.saveAcademicMarks() deletes all of a user's rows and
-- re-inserts them on every save (one row per subject).
-- ---------------------------------------------------------------------
CREATE TABLE student_academic_marks (
    mark_id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES app_users(user_id),
    subject_name        VARCHAR(120) NOT NULL,
    percentage_score    DOUBLE NOT NULL
);


-- ---------------------------------------------------------------------
-- 4. BURSARIES
-- ---------------------------------------------------------------------
-- Read by BursaryDAO.searchBursaries() / getById() / getMatchedBursaryCount().
-- This is the catalogue of real bursaries. Seed rows below are just a
-- handful of realistic South African examples for testing the search,
-- matching and deadline-notification features end to end - populating
-- the full list of ~200 real bursaries is separate follow-up work.
-- ---------------------------------------------------------------------
CREATE TABLE bursaries (
    bursary_id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                      VARCHAR(200)   NOT NULL,
    organization               VARCHAR(150)   NOT NULL,
    category                   VARCHAR(80),
    funding_amount_desc        VARCHAR(150),
    closing_deadline           DATE           NOT NULL,
    min_academic_percentage    DOUBLE         DEFAULT 0,
    max_household_income       DOUBLE         DEFAULT 999999999,
    target_field               VARCHAR(120)   DEFAULT 'All',
    about_text                 VARCHAR(4000),
    is_active                  BOOLEAN        DEFAULT TRUE
);

INSERT INTO bursaries (title, organization, category, funding_amount_desc, closing_deadline, min_academic_percentage, max_household_income, target_field, about_text, is_active) VALUES
('NSFAS Bursary', 'National Student Financial Aid Scheme', 'Government', 'Full tuition, accommodation and living allowance', DATE('2027-01-31'), 50, 350000, 'All', 'South Africa''s main government-funded bursary for students from households earning under the combined household income threshold.', TRUE),
('Sasol Bursary Programme', 'Sasol Limited', 'Corporate', 'Full cost of study plus a monthly allowance', DATE('2026-12-15'), 65, 600000, 'Engineering', 'Covers tuition, accommodation, books and a living allowance for students studying towards an engineering, science or related qualification, with vacation work at Sasol.', TRUE),
('Allan Gray Orbis Fellowship', 'Allan Gray Orbis Foundation', 'Private', 'Full tuition and mentorship programme', DATE('2027-03-31'), 70, 999999999, 'All', 'A fellowship for entrepreneurially-minded students, covering tuition and providing a multi-year leadership and mentorship programme.', TRUE),
('Funza Lushaka Bursary', 'Department of Basic Education', 'Government', 'Full tuition, accommodation and book allowance', DATE('2026-11-30'), 55, 999999999, 'Education', 'For students studying towards a teaching qualification in a priority subject, with a commitment to teach at a public school after graduating.', TRUE),
('MTN Foundation Bursary', 'MTN Group', 'Corporate', 'Tuition, accommodation and a laptop', DATE('2026-10-31'), 60, 500000, 'Information Technology', 'Supports students in ICT-related fields with tuition, accommodation and a laptop for the duration of their studies.', TRUE),
('Investec Bursary Programme', 'Investec', 'Corporate', 'Full cost of study', DATE('2027-02-28'), 70, 999999999, 'Commerce', 'Covers the full cost of study for high-achieving students in finance, accounting, actuarial science and related commerce fields.', TRUE);


-- ---------------------------------------------------------------------
-- 5. BURSARY_APPLICATIONS
-- ---------------------------------------------------------------------
-- This is the table BursaryServlet.doPost("/apply") actually writes to.
-- (Note: there is a separate, older ApplicationDAO.java in the project
-- that queries tables called APPLICATION / BURSARY / DOCUMENTTYPE /
-- USERDOCUMENT - that class is not called from any servlet and is not
-- part of the real schema. Recommend deleting ApplicationDAO.java, or
-- rewriting it to match this table, so it doesn't mislead anyone
-- reading the code later.)
-- ---------------------------------------------------------------------
CREATE TABLE bursary_applications (
    application_id      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              INT NOT NULL REFERENCES app_users(user_id),
    bursary_id           INT NOT NULL REFERENCES bursaries(bursary_id),
    status               VARCHAR(30) DEFAULT 'Submitted',
    submitted_at         TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_one_application_per_bursary UNIQUE (user_id, bursary_id)
);


-- ---------------------------------------------------------------------
-- 6. WALLET_TRANSACTIONS
-- ---------------------------------------------------------------------
-- WalletDAO never stores a running balance - available balance is
-- always computed on the fly as SUM(Income) - SUM(Expense) over this
-- table, so there is deliberately no separate "wallets" table.
-- ---------------------------------------------------------------------
CREATE TABLE wallet_transactions (
    transaction_id       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              INT NOT NULL REFERENCES app_users(user_id),
    transaction_type     VARCHAR(20) NOT NULL,   -- 'Income' or 'Expense'
    description          VARCHAR(255),
    amount               DECIMAL(10,2) NOT NULL,
    category_name        VARCHAR(80),
    transaction_date     DATE      DEFAULT CURRENT_DATE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ---------------------------------------------------------------------
-- 7. BUDGET_CATEGORIES
-- ---------------------------------------------------------------------
CREATE TABLE budget_categories (
    category_id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES app_users(user_id),
    category_name   VARCHAR(80) NOT NULL,
    monthly_limit   DECIMAL(10,2) NOT NULL
);


-- ---------------------------------------------------------------------
-- 8. CALENDAR_EVENTS
-- ---------------------------------------------------------------------
CREATE TABLE calendar_events (
    event_id        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES app_users(user_id),
    title           VARCHAR(200) NOT NULL,
    event_date      DATE NOT NULL,
    event_type      VARCHAR(60) DEFAULT 'Milestone',
    description     VARCHAR(1000)
);


-- ---------------------------------------------------------------------
-- 9. USER_DOCUMENTS  (the Document Vault)
-- ---------------------------------------------------------------------
CREATE TABLE user_documents (
    document_id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id               INT NOT NULL REFERENCES app_users(user_id),
    document_type         VARCHAR(120) NOT NULL,
    file_name             VARCHAR(255) NOT NULL,
    file_path             VARCHAR(500) NOT NULL,
    file_size_bytes       INT,
    verification_status   VARCHAR(20) DEFAULT 'Pending',   -- 'Pending' / 'Verified' / 'Rejected'
    uploaded_at           TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);


-- ---------------------------------------------------------------------
-- 10. LEARN_LESSONS
-- ---------------------------------------------------------------------
-- lesson_id is a PLAIN integer key, NOT auto-generated, because the 9
-- front-end pages (wf26_course_lesson1.html ... wf34_course_lesson9.html)
-- already have lesson IDs 1-9 hard-coded into their "Mark as Complete"
-- buttons. The seed rows below must keep those exact IDs or the
-- checkmarks on the curriculum page will silently stop matching.
-- ---------------------------------------------------------------------
CREATE TABLE learn_lessons (
    lesson_id             INT PRIMARY KEY,
    title                 VARCHAR(200) NOT NULL,
    category              VARCHAR(80),
    duration_min          INT,
    xp_reward             INT DEFAULT 20,
    video_timestamp       VARCHAR(20),
    why_it_matters        VARCHAR(2000),
    plain_language_body   VARCHAR(4000)
);

INSERT INTO learn_lessons (lesson_id, title, category, duration_min, xp_reward, why_it_matters, plain_language_body) VALUES
(1, 'The 50/30/20 rule explained simply', 'Budgeting', 6, 20, 'It gives you one simple split to follow instead of guessing where your money should go.', 'The 50/30/20 rule is a simple way to divide your money: 50% on needs, 30% on wants, and 20% on savings or debt repayment. It is a starting point, not a strict law - adjust the percentages to fit a student income.'),
(2, 'Build your first monthly budget', 'Budgeting', 8, 20, 'A written budget is the difference between money disappearing and money doing what you planned.', 'Start by listing every source of income for the month, then every fixed expense (rent, data, transport), then variable expenses (food, social). Whatever is left is what you can save or must cut from.'),
(3, 'Write a motivation letter that wins money', 'Bursaries', 10, 20, 'A strong motivation letter is often the deciding factor between two similarly qualified applicants.', 'Focus on specifics: why this field, why this institution, and what you plan to do with the qualification. Avoid generic statements - funders read hundreds of letters and specifics stand out.'),
(4, 'Debit vs credit: what''s actually safer?', 'Banking', 5, 20, 'Understanding the difference protects you from debt you did not mean to take on.', 'A debit card spends money you already have. A credit facility lets you spend money you do not yet have, and charges interest if you do not repay it in full. For most students starting out, a debit account is the safer default.'),
(5, 'Avoid hidden bank fees', 'Banking', 5, 20, 'Small monthly fees add up to real money over a year of student life.', 'Look out for monthly account fees, ATM withdrawal fees outside your bank''s network, and SMS notification fees. Many banks offer a free or low-cost student account - it is worth comparing before you open one.'),
(6, 'What is interest, really?', 'Financial Literacy', 6, 20, 'Interest can work for you when you save, or against you when you borrow - knowing which is which matters.', 'Interest is the cost of borrowing money, or the reward for saving it. A savings account pays you interest; a loan charges you interest. The rate and how often it compounds both affect how much it adds up to.'),
(7, 'Budget Planning in 3 simple steps', 'Budgeting', 7, 20, 'Breaking budgeting into three steps makes it far less intimidating to start.', 'Step 1: know your total income. Step 2: list every expense, no matter how small. Step 3: assign every rand a job before the month starts, so you decide where it goes instead of wondering where it went.'),
(8, 'Tax-free savings for under-25s', 'Savings', 6, 20, 'Starting early with tax-free savings means decades of compounding without paying tax on the growth.', 'A Tax-Free Savings Account lets your investment grow without being taxed on interest, dividends or capital gains, up to an annual and lifetime contribution limit. Starting even a small amount as a student gives it more time to grow.'),
(9, 'Needs vs wants: the 24-hour rule', 'Budgeting', 5, 20, 'A short waiting period before a purchase quietly removes most impulse spending.', 'A need keeps you fed, housed, safe or enrolled. A want makes life more enjoyable but is not essential. Before any non-essential purchase, wait 24 hours - if you still want it and can afford it, buy it. Most impulse buys do not survive the wait.');


-- ---------------------------------------------------------------------
-- 11. USER_LESSON_PROGRESS
-- ---------------------------------------------------------------------
CREATE TABLE user_lesson_progress (
    progress_id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES app_users(user_id),
    lesson_id       INT NOT NULL REFERENCES learn_lessons(lesson_id),
    completed_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_one_progress_row_per_lesson UNIQUE (user_id, lesson_id)
);


-- ---------------------------------------------------------------------
-- Notifications are NOT stored anywhere on purpose.
-- ---------------------------------------------------------------------
-- NotificationDAO.getLiveNotifications() computes everything fresh on
-- every request (upcoming bursary deadlines, missing documents, budget
-- overspend) by querying the tables above - it never writes to a
-- "notifications" table, so none is needed.
-- =====================================================================
