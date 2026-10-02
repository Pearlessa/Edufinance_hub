/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.dao;

import za.ac.cput.eduhub.model.CalendarEvent;
import za.ac.cput.eduhub.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CalendarDAO {

    public List<CalendarEvent> getEventsByUser(int userId) throws SQLException {
        List<CalendarEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM calendar_events WHERE user_id = ? ORDER BY event_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CalendarEvent e = new CalendarEvent();
                    e.setEventId(rs.getInt("event_id"));
                    e.setUserId(userId);
                    e.setTitle(rs.getString("title"));
                    e.setEventDate(rs.getDate("event_date"));
                    e.setEventType(rs.getString("event_type"));
                    e.setDescription(rs.getString("description"));
                    events.add(e);
                }
            }
        }
        return events;
    }

    /**
     * Auto-populates the calendar with real, currently-relevant reminders:
     * upcoming deadlines for bursaries matching the student's profile, and
     * any mandatory documents still missing from their vault. Safe to call
     * every time the calendar page loads - addEvent()'s own duplicate check
     * (same user, title and date) means calling this repeatedly never
     * creates repeat entries for something already on the calendar.
     */
    public void syncAutoEvents(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Upcoming bursary deadlines matching this student's field of study
            String bursarySql =
                "SELECT b.title, b.closing_deadline FROM bursaries b " +
                "INNER JOIN student_profiles p ON (b.target_field = p.field_of_study OR b.target_field = 'All') " +
                "WHERE p.user_id = ? AND b.closing_deadline >= CURRENT_DATE " +
                "AND b.is_active = TRUE";

            List<CalendarEvent> toInsert = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(bursarySql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CalendarEvent ev = new CalendarEvent();
                        ev.setUserId(userId);
                        ev.setTitle("Bursary Deadline: " + rs.getString("title"));
                        ev.setEventDate(rs.getDate("closing_deadline"));
                        ev.setEventType("Bursary Deadline");
                        ev.setDescription("Application closing date for " + rs.getString("title") + ".");
                        toInsert.add(ev);
                    }
                }
            }

            // Missing mandatory documents
            String presentDocsSql = "SELECT document_type FROM user_documents WHERE user_id = ? AND verification_status = 'Verified'";
            List<String> presentTypes = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(presentDocsSql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) presentTypes.add(rs.getString("document_type"));
                }
            }
            for (String required : DocumentDAO.MANDATORY_DOCS) {
                if (!presentTypes.contains(required)) {
                    CalendarEvent ev = new CalendarEvent();
                    ev.setUserId(userId);
                    ev.setTitle("Missing Document: " + required);
                    ev.setEventDate(new java.sql.Date(System.currentTimeMillis()));
                    ev.setEventType("Document Reminder");
                    ev.setDescription("Your " + required + " is still missing from your vault.");
                    toInsert.add(ev);
                }
            }

            for (CalendarEvent ev : toInsert) {
                addEvent(ev); // its own duplicate check silently skips anything already scheduled
            }
        }
    }

    public boolean addEvent(CalendarEvent ev) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM calendar_events WHERE user_id = ? AND title = ? AND event_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, ev.getUserId());
            checkPs.setString(2, ev.getTitle());
            checkPs.setDate(3, ev.getEventDate());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return false; // Prevent duplicate schedule entry
                }
            }
        }

        String sql = "INSERT INTO calendar_events (user_id, title, event_date, event_type, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ev.getUserId());
            ps.setString(2, ev.getTitle());
            ps.setDate(3, ev.getEventDate());
            ps.setString(4, ev.getEventType());
            ps.setString(5, ev.getDescription());
            return ps.executeUpdate() > 0;
        }
    }
}