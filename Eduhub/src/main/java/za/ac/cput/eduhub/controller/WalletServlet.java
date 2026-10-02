/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.controller;

import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import za.ac.cput.eduhub.dao.WalletDAO;
import za.ac.cput.eduhub.model.Transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

@WebServlet(name = "WalletServlet", urlPatterns = {"/api/wallet/*"})
public class WalletServlet extends HttpServlet {
    private final WalletDAO walletDAO = new WalletDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        String path = req.getPathInfo();
        try {
            if ("/dashboard".equals(path)) {
                // No opening balance is ever assumed - both figures are a real
                // SUM() over wallet_transactions, so a brand new user with no
                // transactions correctly gets R0.00 across the board, not a
                // placeholder amount.
                BigDecimal income = walletDAO.getTotalIncome(userId);
                BigDecimal expenses = walletDAO.getTotalExpenses(userId);
                BigDecimal balance = income.subtract(expenses);
                List<Map<String, Object>> distribution = walletDAO.getSpendingByCategory(userId);
                List<Map<String, Object>> weekly = walletDAO.getWeeklySpending(userId);

                Map<String, Object> data = new HashMap<>();
                data.put("availableBalance", balance);
                data.put("monthlyIncome", income);
                data.put("monthlyExpenses", expenses);
                data.put("distribution", distribution);
                data.put("weeklySpending", weekly);
                data.put("hasAnyTransactions", income.signum() > 0 || expenses.signum() > 0);
                out.print(gson.toJson(data));
            } else if ("/categories".equals(path)) {
                List<Map<String, Object>> categories = walletDAO.getCategoryBudgetsAndSpending(userId);
                out.print(gson.toJson(categories));
            } else if ("/trends".equals(path)) {
                List<Map<String, Object>> trends = walletDAO.getMonthlyTrends(userId, 6);
                out.print(gson.toJson(trends));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"error\": \"Unknown wallet endpoint.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        String path = req.getPathInfo();
        try {
            if ("/transaction".equals(path)) {
                // Adding an "Income" transaction here IS how a user tops up
                // their balance - there is deliberately no separate opening
                // balance field, since the balance is always the running sum
                // of every transaction ever logged.
                String type = trimOrDefault(req.getParameter("type"), "Expense");
                String desc = trimOrDefault(req.getParameter("description"), "");
                String amountStr = req.getParameter("amount");
                String category = trimOrDefault(req.getParameter("category"), "Other");

                if (desc.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Please enter a description.\"}");
                    return;
                }
                if (amountStr == null || amountStr.trim().isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Please enter an amount.\"}");
                    return;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr.trim());
                } catch (NumberFormatException nfe) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Amount must be a valid number.\"}");
                    return;
                }
                if (amount.signum() <= 0) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Amount must be greater than zero.\"}");
                    return;
                }

                Transaction t = new Transaction();
                t.setUserId(userId);
                t.setTransactionType(type);
                t.setDescription(desc);
                t.setAmount(amount);
                t.setCategoryName(category);

                walletDAO.insertTransaction(t);
                out.print("{\"success\": true, \"message\": \"Transaction logged.\"}");
            } else if ("/category".equals(path)) {
                String name = trimOrDefault(req.getParameter("categoryName"), "");
                String limitStr = req.getParameter("limit");

                if (name.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Please enter a category name.\"}");
                    return;
                }
                BigDecimal limit;
                try {
                    limit = new BigDecimal(limitStr == null ? "" : limitStr.trim());
                } catch (NumberFormatException nfe) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print("{\"success\": false, \"message\": \"Monthly limit must be a valid number.\"}");
                    return;
                }

                walletDAO.insertBudgetCategory(userId, name, limit);
                out.print("{\"success\": true, \"message\": \"Budget ceiling saved.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print("{\"success\": false, \"message\": \"Unknown wallet endpoint.\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + escape(e.getMessage()) + "\"}");
        }
    }

    private String trimOrDefault(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    /** Every hand-built JSON error string in this class runs through this -
     *  an unescaped exception message containing a quote character (Java 14+
     *  NPE "helpful messages" often do) would otherwise produce invalid JSON
     *  that the client's JSON.parse() then fails on. */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private int resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("userId") != null) ? (int) s.getAttribute("userId") : 1;
    }
}
