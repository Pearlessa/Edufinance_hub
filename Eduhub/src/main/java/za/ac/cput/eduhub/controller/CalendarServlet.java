/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.controller;

import com.google.gson.Gson;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import za.ac.cput.eduhub.dao.CalendarDAO;
import za.ac.cput.eduhub.model.CalendarEvent;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "CalendarServlet", urlPatterns = {"/api/calendar/*"})
public class CalendarServlet extends HttpServlet {
    private final CalendarDAO calendarDAO = new CalendarDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        try {
            calendarDAO.syncAutoEvents(userId); // upcoming bursary deadlines + missing docs, added if not already scheduled
            List<CalendarEvent> events = calendarDAO.getEventsByUser(userId);
            List<Map<String, String>> outList = new ArrayList<>();

            for (CalendarEvent e : events) {
                Map<String, String> item = new HashMap<>();
                item.put("title", e.getTitle());
                item.put("date", e.getEventDate().toString());
                item.put("type", e.getEventType());
                item.put("description", e.getDescription());
                outList.add(item);
            }
            out.print(gson.toJson(outList));
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

        String title = req.getParameter("title");
        String dateStr = req.getParameter("date");
        String type = req.getParameter("type");
        String description = req.getParameter("description");

        CalendarEvent ev = new CalendarEvent();
        ev.setUserId(userId);
        ev.setTitle(title.trim());
        ev.setEventDate(Date.valueOf(dateStr));
        ev.setEventType(type != null ? type.trim() : "Milestone");
        ev.setDescription(description != null ? description.trim() : "");

        try {
            boolean added = calendarDAO.addEvent(ev);
            if (added) {
                out.print("{\"success\": true, \"message\": \"Event added to schedule.\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\": false, \"message\": \"An identical event already exists on this date.\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    private int resolveUserId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s != null && s.getAttribute("userId") != null) ? (int) s.getAttribute("userId") : 1;
    }
}