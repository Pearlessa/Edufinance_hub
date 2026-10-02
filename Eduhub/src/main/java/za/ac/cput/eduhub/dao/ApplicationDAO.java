/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.Application;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Comaro Crossing
 */

public class ApplicationDAO {

    public boolean submitApplication(int userId, int bursaryId) throws SQLException {
        if (!hasUploadedRequiredDocuments(userId)) {
            throw new SQLException("Application Blocked: Missing mandatory documents in the Document Vault.");
        }

        String checkDupSql = "SELECT application_id FROM APPLICATION WHERE user_id = ? AND bursary_id = ?";
        String insertSql = "INSERT INTO APPLICATION (user_id, bursary_id, status) VALUES (?, ?, 'Submitted')";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement psCheck = conn.prepareStatement(checkDupSql)) {
                psCheck.setInt(1, userId);
                psCheck.setInt(2, bursaryId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("Application already submitted for this bursary opportunity.");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, bursaryId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public List<Application> getApplicationsByUser(int userId) throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.*, b.name AS bursary_name FROM APPLICATION a JOIN BURSARY b ON a.bursary_id = b.bursary_id WHERE a.user_id = ? ORDER BY a.submitted_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Application app = new Application(
                        rs.getInt("application_id"),
                        rs.getInt("user_id"),
                        rs.getInt("bursary_id"),
                        rs.getString("status"),
                        rs.getTimestamp("submitted_at")
                    );
                    app.setBursaryName(rs.getString("bursary_name"));
                    list.add(app);
                }
            }
        }
        return list;
    }

    private boolean hasUploadedRequiredDocuments(int userId) throws SQLException {
        String sql = "SELECT COUNT(dt.document_type_id) AS missing_count FROM DOCUMENTTYPE dt WHERE dt.is_required = TRUE AND dt.document_type_id NOT IN (SELECT ud.document_type_id FROM USERDOCUMENT ud WHERE ud.user_id = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("missing_count") == 0;
                }
            }
        }
        return false;
    }
}
