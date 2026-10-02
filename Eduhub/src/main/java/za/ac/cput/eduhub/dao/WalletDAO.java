/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.Transaction;
import za.ac.cput.eduhub.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class WalletDAO {

    public BigDecimal getTotalIncome(int userId) throws SQLException {
        String sql = "SELECT SUM(amount) FROM wallet_transactions WHERE user_id = ? AND transaction_type = 'Income'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalExpenses(int userId) throws SQLException {
        String sql = "SELECT SUM(amount) FROM wallet_transactions WHERE user_id = ? AND transaction_type = 'Expense'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBigDecimal(1) != null) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public void insertTransaction(Transaction t) throws SQLException {
        String sql = "INSERT INTO wallet_transactions (user_id, transaction_type, description, amount, category_name, transaction_date) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_DATE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getTransactionType());
            ps.setString(3, t.getDescription());
            ps.setBigDecimal(4, t.getAmount());
            ps.setString(5, t.getCategoryName());
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getSpendingByCategory(int userId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT category_name, SUM(amount) AS total FROM wallet_transactions " +
                     "WHERE user_id = ? AND transaction_type = 'Expense' GROUP BY category_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", rs.getString("category_name"));
                    map.put("amount", rs.getBigDecimal("total"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getCategoryBudgetsAndSpending(int userId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT b.category_name, b.monthly_limit, " +
                     "COALESCE((SELECT SUM(amount) FROM wallet_transactions t " +
                     "          WHERE t.user_id = b.user_id AND t.category_name = b.category_name AND t.transaction_type = 'Expense'), 0) AS spent " +
                     "FROM budget_categories b WHERE b.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    BigDecimal limit = rs.getBigDecimal("monthly_limit");
                    BigDecimal spent = rs.getBigDecimal("spent");
                    map.put("category", rs.getString("category_name"));
                    map.put("limit", limit);
                    map.put("spent", spent);
                    map.put("isOver", spent.compareTo(limit) > 0);
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Real per-day expense totals for the current week (Monday-Sunday), for
     * the "Weekly Spending" bars on the wallet overview page. Returns one
     * entry per day in order, zero-filled for days with no transactions -
     * never fabricated placeholder numbers.
     */
    public List<Map<String, Object>> getWeeklySpending(int userId) throws SQLException {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);

        Map<java.time.LocalDate, BigDecimal> totals = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            totals.put(monday.plusDays(i), BigDecimal.ZERO);
        }

        String sql = "SELECT transaction_date, SUM(amount) AS total FROM wallet_transactions " +
                     "WHERE user_id = ? AND transaction_type = 'Expense' " +
                     "AND transaction_date >= ? AND transaction_date <= ? " +
                     "GROUP BY transaction_date";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(monday));
            ps.setDate(3, java.sql.Date.valueOf(monday.plusDays(6)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate d = rs.getDate("transaction_date").toLocalDate();
                    totals.put(d, rs.getBigDecimal("total"));
                }
            }
        }

        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<Map<String, Object>> result = new ArrayList<>();
        int i = 0;
        for (Map.Entry<java.time.LocalDate, BigDecimal> e : totals.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("day", dayLabels[i++]);
            row.put("amount", e.getValue());
            result.add(row);
        }
        return result;
    }

    /**
     * Real month-by-month income/expense/savings for the last `monthsBack`
     * months (this month included), for the wallet Trends chart. Months
     * with no transactions come back zero-filled rather than being skipped,
     * so the chart always shows a continuous, honest timeline.
     */
    public List<Map<String, Object>> getMonthlyTrends(int userId, int monthsBack) throws SQLException {
        java.time.YearMonth current = java.time.YearMonth.now();
        LinkedHashMap<java.time.YearMonth, BigDecimal[]> totals = new LinkedHashMap<>(); // [income, expense]
        for (int i = monthsBack - 1; i >= 0; i--) {
            totals.put(current.minusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        java.time.YearMonth earliest = current.minusMonths(monthsBack - 1);

        String sql = "SELECT YEAR(transaction_date) AS yr, MONTH(transaction_date) AS mo, " +
                     "transaction_type, SUM(amount) AS total FROM wallet_transactions " +
                     "WHERE user_id = ? AND transaction_date >= ? " +
                     "GROUP BY YEAR(transaction_date), MONTH(transaction_date), transaction_type";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, java.sql.Date.valueOf(earliest.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.YearMonth ym = java.time.YearMonth.of(rs.getInt("yr"), rs.getInt("mo"));
                    BigDecimal[] pair = totals.get(ym);
                    if (pair == null) continue; // outside our window, ignore
                    if ("Income".equalsIgnoreCase(rs.getString("transaction_type"))) {
                        pair[0] = rs.getBigDecimal("total");
                    } else {
                        pair[1] = rs.getBigDecimal("total");
                    }
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<java.time.YearMonth, BigDecimal[]> e : totals.entrySet()) {
            BigDecimal income = e.getValue()[0];
            BigDecimal expense = e.getValue()[1];
            Map<String, Object> row = new HashMap<>();
            row.put("label", e.getKey().getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
            row.put("income", income);
            row.put("expenses", expense);
            row.put("savings", income.subtract(expense));
            result.add(row);
        }
        return result;
    }

    public void insertBudgetCategory(int userId, String name, BigDecimal limit) throws SQLException {
        String sql = "INSERT INTO budget_categories (user_id, category_name, monthly_limit) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setBigDecimal(3, limit);
            ps.executeUpdate();
        }
    }
}