package za.ac.cput.eduhub.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.cput.eduhub.dao.UserDAO;
import za.ac.cput.eduhub.model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/register", "/api/auth/login", "/api/auth/logout", "/api/auth/*"})
public class AuthServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        setCorsHeaders(resp);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        // Top-level barrier prevents Tomcat 9 from generating an HTML 500 page
        try {
            String uri = req.getRequestURI();
            if (uri == null) uri = "";
            uri = uri.toLowerCase();

            if (uri.endsWith("register")) {
                handleRegister(req, resp, out);
            } else if (uri.endsWith("login")) {
                handleLogin(req, resp, out);
            } else if (uri.endsWith("logout")) {
                handleLogout(req, resp, out);
            } else {
                sendJson(resp, out, false, "Requested endpoint not found: " + uri);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            sendJson(resp, out, false, "Server processing error: " + t.getMessage());
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        String studentType = req.getParameter("studentType");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (isEmpty(fullName) || isEmpty(email) || isEmpty(phone) || isEmpty(studentType) || isEmpty(password)) {
            sendJson(resp, out, false, "All registration fields are mandatory.");
            return;
        }

        if (confirmPassword != null && !confirmPassword.trim().isEmpty() && !password.equals(confirmPassword)) {
            sendJson(resp, out, false, "Passwords do not match. Please verify your password entry.");
            return;
        }

        if (password.length() < 8) {
            sendJson(resp, out, false, "Password must be at least 8 characters in length.");
            return;
        }

        try {
            // Verify if user already exists in the NetBeans database
            User existing = userDAO.getUserByEmail(email.trim().toLowerCase());
            if (existing != null) {
                sendJson(resp, out, false, "This email address is already registered in the database.");
                return;
            }

            // Persist user directly to APP.APP_USERS in Apache Derby
            User user = new User();
            user.setFullName(fullName.trim());
            user.setEmail(email.trim().toLowerCase());
            user.setPhoneNumber(phone.trim());
            user.setStudentType(studentType.trim());

            int newUserId = userDAO.createUser(user, password);
            if (newUserId > 0) {
                sendJson(resp, out, true, "Account created successfully in database.");
            } else {
                sendJson(resp, out, false, "Database rejected insertion. Check table constraints.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendJson(resp, out, false, "Database error: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            sendJson(resp, out, false, "Registration failure: " + t.getMessage());
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        if (isEmpty(email) || isEmpty(password)) {
            sendJson(resp, out, false, "Please enter both your email address and password.");
            return;
        }

        try {
            // 1. Look up user by email in the database
            User user = userDAO.getUserByEmail(email.trim().toLowerCase());
            if (user == null) {
                sendJson(resp, out, false, "Account not found with this email address.");
                return;
            }

            // 2. Authenticate the password hash against the database
            User authenticated = userDAO.authenticate(email.trim().toLowerCase(), password);
            if (authenticated != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", authenticated.getUserId());
                session.setAttribute("userName", authenticated.getFullName());
                session.setAttribute("profileCompleted", authenticated.isProfileCompleted());

                resp.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"success\":true,\"userId\":" + authenticated.getUserId() +
                          ",\"userName\":\"" + escapeJson(authenticated.getFullName()) + "\"" +
                          ",\"profileCompleted\":" + authenticated.isProfileCompleted() + "}");
            } else {
                sendJson(resp, out, false, "Incorrect password.");
            }
        } catch (IllegalStateException e) {
            sendJson(resp, out, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            sendJson(resp, out, false, "Database error: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            sendJson(resp, out, false, "Login failure: " + t.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sendJson(resp, out, true, "Logged out successfully.");
    }

    private void sendJson(HttpServletResponse resp, PrintWriter out, boolean success, String message) {
        resp.setStatus(HttpServletResponse.SC_OK);
        out.print("{\"success\":" + success + ",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}