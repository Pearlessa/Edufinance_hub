/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.Bursary;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BursaryDAO {

    public List<Bursary> searchBursaries(String keyword) throws SQLException {
        List<Bursary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM bursaries WHERE closing_deadline >= CURRENT_DATE ");

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(title) LIKE ? OR LOWER(organization) LIKE ? OR LOWER(target_field) LIKE ? OR LOWER(category) LIKE ?) ");
        }
        sql.append("ORDER BY closing_deadline ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                String term = "%" + keyword.trim().toLowerCase() + "%";
                ps.setString(1, term);
                ps.setString(2, term);
                ps.setString(3, term);
                ps.setString(4, term);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBursary(rs));
                }
            }
        }
        return list;
    }

    public Bursary getById(int bursaryId) throws SQLException {
        String sql = "SELECT * FROM bursaries WHERE bursary_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bursaryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBursary(rs);
                }
            }
        }
        return null;
    }

    public int getMatchedBursaryCount(double studentAverage, String fieldOfStudy) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bursaries WHERE min_academic_percentage <= ? AND (target_field = 'All' OR target_field = ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, studentAverage);
            ps.setString(2, fieldOfStudy);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Bursary mapBursary(ResultSet rs) throws SQLException {
        Bursary b = new Bursary();
        b.setBursaryId(rs.getInt("bursary_id"));
        b.setTitle(rs.getString("title"));
        b.setOrganization(rs.getString("organization"));
        b.setCategory(rs.getString("category"));
        b.setFundingAmountDesc(rs.getString("funding_amount_desc"));
        b.setClosingDeadline(rs.getDate("closing_deadline"));
        b.setMinAcademicPercentage(rs.getDouble("min_academic_percentage"));
        b.setMaxHouseholdIncome(rs.getDouble("max_household_income"));
        b.setTargetField(rs.getString("target_field"));
        b.setAboutText(rs.getString("about_text"));
        b.setActive(rs.getBoolean("is_active"));
        return b;
    }
}