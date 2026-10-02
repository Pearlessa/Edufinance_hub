/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.Notification;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> getLiveNotifications(int userId) {
        List<Notification> alerts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Bursary deadlines closing within 14 days
            String bursaryAlertSql = 
                "SELECT b.title, b.closing_deadline, " +
                "{fn TIMESTAMPDIFF(SQL_TSI_DAY, CURRENT_DATE, b.closing_deadline)} AS days_left " +
                "FROM bursaries b " +
                "INNER JOIN student_profiles p ON (b.target_field = p.field_of_study OR b.target_field = 'All') " +
                "WHERE p.user_id = ? " +
                "  AND b.closing_deadline >= CURRENT_DATE " +
                "  AND {fn TIMESTAMPDIFF(SQL_TSI_DAY, CURRENT_DATE, b.closing_deadline)} <= 14";

            try (PreparedStatement ps = conn.prepareStatement(bursaryAlertSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString("title");
                        int days = rs.getInt("days_left");
                        alerts.add(new Notification(
                            userId,
                            "Deadline Approaching: " + title,
                            "Applications close in " + days + " day(s). Check vault prerequisites and submit.",
                            "Bursary",
                            days <= 1 ? "Closing Today" : days + "d left",
                            false
                        ));
                    }
                }
            }

            // 2. Missing compliance documents in vault
            String missingDocSql = "SELECT document_type FROM user_documents WHERE user_id = ? AND verification_status = 'Verified'";
            List<String> presentTypes = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(missingDocSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        presentTypes.add(rs.getString("document_type"));
                    }
                }
            }

            for (String required : DocumentDAO.MANDATORY_DOCS) {
                if (!presentTypes.contains(required)) {
                    alerts.add(new Notification(
                        userId,
                        "Action Needed: Upload " + required,
                        "Your " + required + " is missing from your vault. Upload now to complete your eligibility.",
                        "Application",
                        "Compliance",
                        false
                    ));
                }
            }

            // 3. Category budget overruns
            String budgetAlertSql = 
                "SELECT b.category_name, b.monthly_limit, " +
                "COALESCE(SUM(t.amount), 0) AS total_spend " +
                "FROM budget_categories b " +
                "LEFT JOIN wallet_transactions t ON (b.user_id = t.user_id AND b.category_name = t.category_name AND t.transaction_type = 'Expense') " +
                "WHERE b.user_id = ? " +
                "GROUP BY b.category_name, b.monthly_limit " +
                "HAVING COALESCE(SUM(t.amount), 0) > b.monthly_limit";

            try (PreparedStatement ps = conn.prepareStatement(budgetAlertSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String category = rs.getString("category_name");
                        double limit = rs.getDouble("monthly_limit");
                        double spend = rs.getDouble("total_spend");
                        double variance = spend - limit;

                        alerts.add(new Notification(
                            userId,
                            "Budget Exceeded: " + category,
                            "You have exceeded your monthly limit for " + category + " by R" + String.format("%.2f", variance) + ".",
                            "Wallet",
                            "Over Budget",
                            false
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error generating dynamic notifications: " + e.getMessage());
        }

        return alerts;
    }
}