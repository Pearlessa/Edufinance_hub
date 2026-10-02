package za.ac.cput.eduhub.dao;

import org.junit.jupiter.api.*;
import za.ac.cput.eduhub.model.Bursary;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Read tests for BursaryDAO. BursaryDAO is intentionally read-only from the
 * application's side - bursary rows come from schema.sql / additional_bursaries.sql,
 * not from anything a student does in the UI - so unlike the other DAOs
 * there is no "Create via the DAO" to test here.
 *
 * Rather than testing against whatever bursaries happen to already be
 * seeded (fragile - the count/results would change every time the seed
 * data is refreshed), this class inserts its OWN uniquely-named test
 * bursary directly via SQL in @BeforeAll, tests BursaryDAO against that
 * known row, and deletes it again in @AfterAll so the test never leaves
 * anything behind in the real bursaries table.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BursaryDAOTest {

    private static BursaryDAO bursaryDAO;
    private static int testBursaryId;
    private static final String TEST_TITLE = "JUnit Test Bursary " + System.currentTimeMillis();

    @BeforeAll
    static void setUp() throws SQLException {
        bursaryDAO = new BursaryDAO();

        String sql = "INSERT INTO bursaries (title, organization, category, funding_amount_desc, " +
                     "closing_deadline, min_academic_percentage, max_household_income, target_field, about_text, is_active) " +
                     "VALUES (?, 'JUnit Test Org', 'Testing', 'Full cost of study', ?, 60, 500000, 'Information Technology', 'A test-only bursary row.', TRUE)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, TEST_TITLE);
            ps.setDate(2, Date.valueOf(LocalDate.now().plusDays(30)));
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) testBursaryId = keys.getInt(1);
            }
        }
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        String sql = "DELETE FROM bursaries WHERE bursary_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testBursaryId);
            ps.executeUpdate();
        }
    }

    @Test
    @Order(1)
    @DisplayName("READ: getById() returns the exact bursary just inserted")
    void testGetById() throws SQLException {
        Bursary found = bursaryDAO.getById(testBursaryId);

        assertNotNull(found);
        assertEquals(TEST_TITLE, found.getTitle());
        assertEquals("JUnit Test Org", found.getOrganization());
        assertEquals("Information Technology", found.getTargetField());
        assertTrue(found.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("READ: searchBursaries() finds it by a keyword matching the title")
    void testSearchByTitleKeyword() throws SQLException {
        List<Bursary> results = bursaryDAO.searchBursaries("JUnit Test Bursary");

        boolean found = results.stream().anyMatch(b -> b.getBursaryId() == testBursaryId);
        assertTrue(found, "searchBursaries() should find the test bursary by a title keyword");
    }

    @Test
    @Order(3)
    @DisplayName("READ: searchBursaries() also matches on organization, not just title")
    void testSearchByOrganizationKeyword() throws SQLException {
        List<Bursary> results = bursaryDAO.searchBursaries("JUnit Test Org");

        boolean found = results.stream().anyMatch(b -> b.getBursaryId() == testBursaryId);
        assertTrue(found, "searchBursaries() should also match against the organization column");
    }

    @Test
    @Order(4)
    @DisplayName("READ: searchBursaries() with an empty keyword returns all open bursaries, no filtering")
    void testSearchWithNoKeywordReturnsAll() throws SQLException {
        List<Bursary> results = bursaryDAO.searchBursaries("");

        boolean found = results.stream().anyMatch(b -> b.getBursaryId() == testBursaryId);
        assertTrue(found, "An empty search should behave as \"no filter\" and include the test bursary");
    }

    @Test
    @Order(5)
    @DisplayName("READ: getMatchedBursaryCount() counts it for a matching field + academic average")
    void testGetMatchedBursaryCountMatches() throws SQLException {
        // Test bursary requires min_academic_percentage <= 60 and target_field
        // 'Information Technology' or 'All' - a student averaging 75% in IT
        // should match it.
        int countBefore = bursaryDAO.getMatchedBursaryCount(75.0, "Information Technology");
        assertTrue(countBefore >= 1, "A 75% IT student should match at least the test bursary (min 60%, IT field)");
    }

    @Test
    @Order(6)
    @DisplayName("READ: getMatchedBursaryCount() excludes it when the student's average is below the requirement")
    void testGetMatchedBursaryCountExcludesLowAverage() throws SQLException {
        // Insert a second, stricter test bursary requiring 90%+ to isolate
        // the assertion from any other real seed data that might also
        // require a high average.
        String sql = "INSERT INTO bursaries (title, organization, category, funding_amount_desc, " +
                     "closing_deadline, min_academic_percentage, max_household_income, target_field, about_text, is_active) " +
                     "VALUES (?, 'JUnit Test Org', 'Testing', 'Full cost of study', ?, 90, 500000, 'Information Technology', 'Strict test bursary.', TRUE)";
        int strictBursaryId;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "JUnit Strict Bursary " + System.currentTimeMillis());
            ps.setDate(2, Date.valueOf(LocalDate.now().plusDays(30)));
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                strictBursaryId = keys.getInt(1);
            }
        }

        try {
            // A 50% average should NOT match a bursary that requires 90%+.
            // We can't assert an exact total count (other real bursaries may
            // exist), so instead we confirm this specific strict bursary
            // doesn't inflate the count for a low-average student by
            // comparing against the count for a qualifying average.
            int lowAverageCount = bursaryDAO.getMatchedBursaryCount(50.0, "Information Technology");
            int highAverageCount = bursaryDAO.getMatchedBursaryCount(95.0, "Information Technology");

            assertTrue(highAverageCount > lowAverageCount,
                    "A 95% average should match at least one more bursary (the strict one) than a 50% average");
        } finally {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM bursaries WHERE bursary_id = ?")) {
                ps.setInt(1, strictBursaryId);
                ps.executeUpdate();
            }
        }
    }
}
