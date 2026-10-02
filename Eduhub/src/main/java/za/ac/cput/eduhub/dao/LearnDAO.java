/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.Lesson;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LearnDAO {

    public List<Lesson> getAllLessons(int userId) throws SQLException {
        List<Lesson> lessons = new ArrayList<>();
        String sql = "SELECT l.*, " +
                     "CASE WHEN p.progress_id IS NOT NULL THEN TRUE ELSE FALSE END AS is_completed " +
                     "FROM learn_lessons l " +
                     "LEFT JOIN user_lesson_progress p ON (l.lesson_id = p.lesson_id AND p.user_id = ?) " +
                     "ORDER BY l.lesson_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Lesson lesson = new Lesson();
                    lesson.setLessonId(rs.getInt("lesson_id"));
                    lesson.setTitle(rs.getString("title"));
                    lesson.setCategory(rs.getString("category"));
                    lesson.setDurationMin(rs.getInt("duration_min"));
                    lesson.setXpReward(rs.getInt("xp_reward"));
                    lesson.setVideoTimestamp(rs.getString("video_timestamp"));
                    lesson.setWhyItMatters(rs.getString("why_it_matters"));
                    lesson.setPlainLanguageBody(rs.getString("plain_language_body"));
                    lesson.setCompleted(rs.getBoolean("is_completed"));
                    lessons.add(lesson);
                }
            }
        }
        return lessons;
    }

    public void markComplete(int userId, int lessonId) throws SQLException {
        String checkSql = "SELECT progress_id FROM user_lesson_progress WHERE user_id = ? AND lesson_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return;
            }
        }

        String sql = "INSERT INTO user_lesson_progress (user_id, lesson_id) VALUES (?, ?)";
        String xpSql = "UPDATE app_users SET xp_points = xp_points + 20 WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, lessonId);
                ps.executeUpdate();
            }
            try (PreparedStatement xpPs = conn.prepareStatement(xpSql)) {
                xpPs.setInt(1, userId);
                xpPs.executeUpdate();
            }
        }
    }
}