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
import za.ac.cput.eduhub.dao.ProfileDAO;
import za.ac.cput.eduhub.dao.UserDAO;
import za.ac.cput.eduhub.util.DnaCalculator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "QuestServlet", urlPatterns = {"/api/quest/*"})
public class QuestServlet extends HttpServlet {
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final BursaryDAO bursaryDAO = new BursaryDAO();
    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        int userId = resolveUserId(req);

        String path = req.getPathInfo();
        try {
            if ("/step1-identity".equals(path)) {
                String fullName = req.getParameter("fullName");
                int age = Integer.parseInt(req.getParameter("age"));
                String province = req.getParameter("province");

                profileDAO.saveIdentity(userId, fullName, age, province);
                out.print("{\"success\": true}");
            } else if ("/step2-academic".equals(path)) {
                String grade = req.getParameter("academicLevel");
                String field = req.getParameter("fieldOfStudy");
                String institution = req.getParameter("institution");

                profileDAO.saveAcademicInfo(userId, grade, field, institution != null ? institution.trim() : "");
                out.print("{\"success\": true}");
            } else if ("/step3-marks".equals(path)) {
                // Body is JSON: {"marks":[{"subject":"Maths","score":85}, ...], "average": 83.5}
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = req.getReader().readLine()) != null) body.append(line);

                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(body.toString()).getAsJsonObject();
                double average = json.get("average").getAsDouble();
                com.google.gson.JsonArray marksArr = json.getAsJsonArray("marks");

                Map<String, Double> marks = new HashMap<>();
                for (com.google.gson.JsonElement el : marksArr) {
                    com.google.gson.JsonObject row = el.getAsJsonObject();
                    marks.put(row.get("subject").getAsString(), row.get("score").getAsDouble());
                }

                profileDAO.saveAcademicMarks(userId, marks, average);
                out.print("{\"success\": true}");
            } else if ("/calculate-dna".equals(path)) {
                double averageScore = Double.parseDouble(req.getParameter("averageScore"));
                String incomeBracket = req.getParameter("incomeBracket");
                String fieldOfStudy = req.getParameter("fieldOfStudy");

                int dnaScore = DnaCalculator.calculateDnaScore(averageScore, incomeBracket);
                profileDAO.saveHouseholdAndDna(userId, incomeBracket, dnaScore);
                userDAO.setProfileCompleted(userId);

                int matchedCount = bursaryDAO.getMatchedBursaryCount(averageScore, fieldOfStudy);

                Map<String, Object> results = new HashMap<>();
                results.put("success", true);
                results.put("dnaScore", dnaScore);
                results.put("academicAverage", averageScore);
                results.put("matchedBursariesCount", matchedCount);
                results.put("potentialFundingValue", "R" + (matchedCount * 125) + ",000");

                out.print(gson.toJson(results));
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