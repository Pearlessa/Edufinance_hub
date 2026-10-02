/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package za.ac.cput.eduhub.controller;

import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import za.ac.cput.eduhub.dao.LearnDAO;
import za.ac.cput.eduhub.model.Lesson;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "LearnServlet", urlPatterns = {"/api/learn/*"})
public class LearnServlet extends HttpServlet {
    private final LearnDAO learnDAO = new LearnDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        try {
            List<Lesson> lessons = learnDAO.getAllLessons(userId);
            out.print(gson.toJson(lessons));
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

        String path = req.getPathInfo();
        if ("/complete".equals(path)) {
            int lessonId = Integer.parseInt(req.getParameter("lessonId"));
            try {
                learnDAO.markComplete(userId, lessonId);
                out.print("{\"success\": true}");
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
            }
        }
    }

    private int resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("userId") != null) ? (int) s.getAttribute("userId") : 1;
    }
}