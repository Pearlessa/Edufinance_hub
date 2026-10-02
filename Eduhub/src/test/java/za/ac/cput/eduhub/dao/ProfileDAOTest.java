package za.ac.cput.eduhub.dao;

import org.junit.jupiter.api.*;
import za.ac.cput.eduhub.model.User;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CRUD tests for ProfileDAO - see the class comment on UserDAOTest for why
 * these are integration tests against a real Derby database.
 *
 * ProfileDAO has no "read" method of its own (no getProfile()) - every
 * method is a write (an upsert or a plain update). So instead of calling a
 * DAO getter to verify results, these tests open a direct, read-only query
 * against student_profiles to check what actually landed in the database.
 * That's a deliberate test-only shortcut, not a workaround for a bug - it's
 * simply the most direct way to verify a DAO that was never given a read
 * method of its own.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileDAOTest {

    private static UserDAO userDAO;
    private static ProfileDAO profileDAO;
    private static int testUserId;

    @BeforeAll
    static void setUp() throws SQLException {
        userDAO = new UserDAO();
        profileDAO = new ProfileDAO();

        User testUser = new User();
        testUser.setFullName("JUnit Profile Tester");
        testUser.setEmail("junit.profile." + System.currentTimeMillis() + "@example.com");
        testUser.setPhoneNumber("0821234567");
        testUser.setStudentType("Undergraduate");
        testUserId = userDAO.createUser(testUser, "TestPassword123!");
    }

    /** Reads a single column back from student_profiles for verification. */
    private Object readProfileColumn(String column) throws SQLException {
        String sql = "SELECT " + column + " FROM student_profiles WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("CREATE: saveIdentity() inserts a new student_profiles row when none exists yet")
    void testSaveIdentityCreatesRow() throws SQLException {
        assertNull(readProfileColumn("full_name"), "Before saveIdentity(), no profile row should exist yet");

        profileDAO.saveIdentity(testUserId, "Leeh Test", 20, "Western Cape");

        assertEquals("Leeh Test", readProfileColumn("full_name"));
        assertEquals("Western Cape", readProfileColumn("province"));
    }

    @Test
    @Order(2)
    @DisplayName("UPDATE: calling saveIdentity() again updates the SAME row instead of inserting a second one")
    void testSaveIdentityUpdatesExistingRow() throws SQLException {
        profileDAO.saveIdentity(testUserId, "Leeh Updated", 21, "Gauteng");

        assertEquals("Leeh Updated", readProfileColumn("full_name"));
        assertEquals("Gauteng", readProfileColumn("province"));

        // Confirm there is still exactly ONE row for this user, not two -
        // this is the whole point of saveIdentity()'s UPDATE-then-fallback-
        // INSERT pattern.
        String countSql = "SELECT COUNT(*) FROM student_profiles WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setInt(1, testUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1), "There should be exactly one profile row per user, never two");
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("UPDATE: saveAcademicInfo() sets grade, field of study and institution")
    void testSaveAcademicInfo() throws SQLException {
        profileDAO.saveAcademicInfo(testUserId, "3rd Year", "Information Technology", "CPUT");

        assertEquals("3rd Year", readProfileColumn("academic_level"));
        assertEquals("Information Technology", readProfileColumn("field_of_study"));
        assertEquals("CPUT", readProfileColumn("institution_name"));
    }

    @Test
    @Order(4)
    @DisplayName("CREATE + UPDATE: saveAcademicMarks() replaces subject marks and updates the average")
    void testSaveAcademicMarks() throws SQLException {
        Map<String, Double> marks = new HashMap<>();
        marks.put("Programming", 85.0);
        marks.put("Databases", 78.0);

        profileDAO.saveAcademicMarks(testUserId, marks, 81.5);

        // The average on student_profiles should reflect what was passed in
        Object avg = readProfileColumn("overall_academic_average");
        assertEquals(81.5, ((Number) avg).doubleValue(), 0.001);

        // And the individual subject rows should exist in student_academic_marks
        String sql = "SELECT COUNT(*) FROM student_academic_marks WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(2, rs.getInt(1), "Both subjects passed in should have their own row");
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("saveAcademicMarks() called again REPLACES old marks rather than accumulating duplicates")
    void testSaveAcademicMarksReplacesOldRows() throws SQLException {
        Map<String, Double> newMarks = new HashMap<>();
        newMarks.put("Information Security", 90.0);

        profileDAO.saveAcademicMarks(testUserId, newMarks, 90.0);

        String sql = "SELECT COUNT(*) FROM student_academic_marks WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1),
                        "Old marks from the previous save should be gone, not accumulated alongside the new one");
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("UPDATE: saveHouseholdAndDna() sets the income bracket and calculated DNA score")
    void testSaveHouseholdAndDna() throws SQLException {
        profileDAO.saveHouseholdAndDna(testUserId, "R86K - R 349K", 74);

        assertEquals("R86K - R 349K", readProfileColumn("household_income_bracket"));
        assertEquals(74, ((Number) readProfileColumn("dna_score")).intValue());
    }
}
