/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package za.ac.cput.eduhub.dao;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import za.ac.cput.eduhub.model.User;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import za.ac.cput.eduhub.model.User;

/**
 *
 * @author Comaro Crossing
 */
 
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserDAOTest {
 
    private static UserDAO userDAO;
    private static int createdUserId;
    private static String testEmail;
 
    @BeforeAll
    static void setUp() {
        userDAO = new UserDAO();
        testEmail = uniqueEmail();
    }
 
    private static String uniqueEmail() {
        return "junit.test." + System.currentTimeMillis() + "@example.com";
    }
 
    @Test
    @Order(1)
    @DisplayName("CREATE: createUser() inserts a new row and returns a real generated user_id")
    void testCreateUser() throws SQLException {
        User newUser = new User();
        newUser.setFullName("JUnit Test Student");
        newUser.setEmail(testEmail);
        newUser.setPhoneNumber("0821234567");
        newUser.setStudentType("Undergraduate");
 
        createdUserId = userDAO.createUser(newUser, "TestPassword123!");
        assertTrue(createdUserId > 0, "createUser() should return a generated user_id greater than 0");
    }
 
    @Test
    @Order(2)
    @DisplayName("READ: getUserById() returns the exact user just created")
    void testGetUserById() throws SQLException {
        User found = userDAO.getUserById(createdUserId);
 
        assertNotNull(found, "getUserById() should find the user created in testCreateUser()");
        assertEquals(testEmail, found.getEmail());
        assertEquals("JUnit Test Student", found.getFullName());
        // The password must never come back in plain text - only the BCrypt hash.
        assertNotEquals("TestPassword123!", found.getPasswordHash());
    }
 
    @Test
    @Order(3)
    @DisplayName("READ: getUserByEmail() finds the same user by email, case-insensitively")
    void testGetUserByEmailIsCaseInsensitive() throws SQLException {
        User found = userDAO.getUserByEmail(testEmail.toUpperCase());
 
        assertNotNull(found, "getUserByEmail() should match regardless of email case");
        assertEquals(createdUserId, found.getUserId());
    }
 
    @Test
    @Order(4)
    @DisplayName("READ + business logic: authenticate() succeeds with the correct password, fails with the wrong one")
    void testAuthenticate() throws SQLException {
        User correct = userDAO.authenticate(testEmail, "TestPassword123!");
        assertNotNull(correct, "authenticate() should succeed with the correct password");
 
        User wrong = userDAO.authenticate(testEmail, "WrongPassword!");
        assertNull(wrong, "authenticate() should return null for an incorrect password");
    }
 
    @Test
    @Order(5)
    @DisplayName("UPDATE: setProfileCompleted() flips profile_completed from false to true")
    void testSetProfileCompleted() throws SQLException {
        User before = userDAO.getUserById(createdUserId);
        assertFalse(before.isProfileCompleted(), "A brand-new user should start with profile_completed = false");
 
        boolean updated = userDAO.setProfileCompleted(createdUserId);
        assertTrue(updated, "setProfileCompleted() should report that a row was updated");
 
        User after = userDAO.getUserById(createdUserId);
        assertTrue(after.isProfileCompleted(), "profile_completed should now be true");
    }
 
    @Test
    @Order(6)
    @DisplayName("UPDATE: addXpPoints() increases xp_points by exactly the given amount")
    void testAddXpPoints() throws SQLException {
        User before = userDAO.getUserById(createdUserId);
        int startingXp = before.getXpPoints();
 
        userDAO.addXpPoints(createdUserId, 20);
 
        User after = userDAO.getUserById(createdUserId);
        assertEquals(startingXp + 20, after.getXpPoints(), "xp_points should increase by exactly 20");
    }
}
 