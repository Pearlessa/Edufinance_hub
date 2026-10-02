package za.ac.cput.eduhub.dao;

import org.mindrot.jbcrypt.BCrypt;
import za.ac.cput.eduhub.model.User;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM app_users WHERE LOWER(TRIM(email)) = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM app_users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        }
        return null;
    }

    public int createUser(User user, String rawPassword) throws SQLException {
        String hashed;
        try {
            hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        } catch (Throwable t) {
            hashed = Integer.toHexString(rawPassword.hashCode());
        }

        String sql = "INSERT INTO app_users (full_name, email, phone_number, student_type, password_hash, failed_attempts, is_locked, profile_completed, xp_points) " +
                     "VALUES (?, ?, ?, ?, ?, 0, false, false, 0)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPhoneNumber());
            ps.setString(4, user.getStudentType());
            ps.setString(5, hashed);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
            return 1;
        }
    }

    public User authenticate(String email, String rawPassword) throws SQLException {
        User user = getUserByEmail(email);
        if (user == null) {
            return null;
        }

        if (user.isLocked()) {
            throw new IllegalStateException("Your account has been locked due to 3 consecutive failed login attempts. Contact support to reset.");
        }

        boolean match = false;
        try {
            match = BCrypt.checkpw(rawPassword, user.getPasswordHash());
        } catch (Throwable t) {
            match = user.getPasswordHash().equals(Integer.toHexString(rawPassword.hashCode())) || 
                    user.getPasswordHash().equals(rawPassword);
        }

        if (match) {
            resetFailedAttempts(user.getUserId());
            return user;
        } else {
            recordFailedAttempt(user.getUserId(), user.getFailedAttempts() + 1);
            return null;
        }
    }

    /**
     * Marks the student onboarding profile as completed when Quest finishes.
     */
    public boolean setProfileCompleted(int userId) throws SQLException {
        String sql = "UPDATE app_users SET profile_completed = true WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Increments XP points for gamified progress actions (lessons, quests, uploads).
     */
    public boolean addXpPoints(int userId, int pointsToAdd) throws SQLException {
        String sql = "UPDATE app_users SET xp_points = xp_points + ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pointsToAdd);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private void recordFailedAttempt(int userId, int newCount) throws SQLException {
        boolean lock = newCount >= 3;
        String sql = "UPDATE app_users SET failed_attempts = ?, is_locked = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newCount);
            ps.setBoolean(2, lock);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private void resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE app_users SET failed_attempts = 0 WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhoneNumber(rs.getString("phone_number"));
        u.setStudentType(rs.getString("student_type"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        u.setLocked(rs.getBoolean("is_locked"));
        u.setProfileCompleted(rs.getBoolean("profile_completed"));
        u.setXpPoints(rs.getInt("xp_points"));
        return u;
    }
}