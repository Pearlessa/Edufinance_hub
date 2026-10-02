/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.Map;

public class ProfileDAO {

    public void saveIdentity(int userId, String fullName, int age, String province) throws SQLException {
        String updateSql = "UPDATE student_profiles SET full_name = ?, age = ?, province = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, fullName);
            ps.setInt(2, age);
            ps.setString(3, province);
            ps.setInt(4, userId);

            if (ps.executeUpdate() == 0) {
                String insertSql = "INSERT INTO student_profiles (user_id, id_or_passport, full_name, age, province, academic_level, field_of_study, household_income_bracket) " +
                                   "VALUES (?, 'PENDING', ?, ?, ?, 'Pending', 'Pending', 'Pending')";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, userId);
                    insertPs.setString(2, fullName);
                    insertPs.setInt(3, age);
                    insertPs.setString(4, province);
                    insertPs.executeUpdate();
                }
            }
        }
    }

    public void saveAcademicInfo(int userId, String grade, String field, String institution) throws SQLException {
        String sql = "UPDATE student_profiles SET academic_level = ?, field_of_study = ?, institution_name = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grade);
            ps.setString(2, field);
            ps.setString(3, institution);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    public void saveAcademicMarks(int userId, Map<String, Double> marks, double average) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM student_academic_marks WHERE user_id = ?")) {
                del.setInt(1, userId);
                del.executeUpdate();
            }

            String ins = "INSERT INTO student_academic_marks (user_id, subject_name, percentage_score) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (Map.Entry<String, Double> entry : marks.entrySet()) {
                    ps.setInt(1, userId);
                    ps.setString(2, entry.getKey());
                    ps.setDouble(3, entry.getValue());
                    ps.executeUpdate();
                }
            }

            String updateAvg = "UPDATE student_profiles SET overall_academic_average = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
            try (PreparedStatement avgPs = conn.prepareStatement(updateAvg)) {
                avgPs.setDouble(1, average);
                avgPs.setInt(2, userId);
                avgPs.executeUpdate();
            }
        }
    }

    public void saveHouseholdAndDna(int userId, String incomeBracket, int dnaScore) throws SQLException {
        String sql = "UPDATE student_profiles SET household_income_bracket = ?, dna_score = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, incomeBracket);
            ps.setInt(2, dnaScore);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }
}