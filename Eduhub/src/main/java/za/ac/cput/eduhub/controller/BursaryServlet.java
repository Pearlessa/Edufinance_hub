/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package za.ac.cput.eduhub.controller;

import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import za.ac.cput.eduhub.dao.BursaryDAO;
import za.ac.cput.eduhub.dao.DocumentDAO;
import za.ac.cput.eduhub.model.Bursary;
import za.ac.cput.eduhub.util.DBConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "BursaryServlet", urlPatterns = {"/api/bursaries/*"})
public class BursaryServlet extends HttpServlet {
    private final BursaryDAO bursaryDAO = new BursaryDAO();
    private final DocumentDAO documentDAO = new DocumentDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        String path = req.getPathInfo();
        try {
            if (path == null || "/search".equals(path) || "/list".equals(path)) {
                // Rule 1: Check onboarding completion before bursary matching
                if (userId != -1 && !isProfileComplete(userId)) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"error\": \"Onboarding incomplete. Complete your profile to unlock matches.\"}");
                    return;
                }
                String q = req.getParameter("q");
                List<Bursary> results = bursaryDAO.searchBursaries(q);
                out.print(gson.toJson(results));
            } else if (path.startsWith("/details/")) {
                int id = Integer.parseInt(path.substring("/details/".length()));
                Bursary b = bursaryDAO.getById(id);
                out.print(gson.toJson(b));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        if (userId <= 0) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"Please log in to apply.\"}");
            return;
        }

        String path = req.getPathInfo();
        if ("/apply".equals(path)) {
            int bursaryId = Integer.parseInt(req.getParameter("bursaryId"));
            try {
                // Rule 3: blockIfMissingDocs
                if (!documentDAO.hasAllRequiredDocuments(userId)) {
                    resp.setStatus(412); // Precondition Failed
                    out.print("{\"success\": false, \"message\": \"Document Vault missing required certified files.\"}");
                    return;
                }

                String sql = "INSERT INTO bursary_applications (user_id, bursary_id) VALUES (?, ?)";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, bursaryId);
                    ps.executeUpdate();
                }
                out.print("{\"success\": true, \"message\": \"Application officially recorded in your portal!\"}");
            } catch (SQLException e) {
                out.print("{\"success\": false, \"message\": \"Application error: " + e.getMessage() + "\"}");
            }
        }
    }

    private boolean isProfileComplete(int userId) throws SQLException {
        String sql = "SELECT profile_completed FROM app_users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    private int resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("userId") != null) ? (int) s.getAttribute("userId") : 1;
    }
}