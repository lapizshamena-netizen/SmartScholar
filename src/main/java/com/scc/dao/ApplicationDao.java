package com.scc.dao;

import com.scc.model.Application;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApplicationDao {

    public boolean submit(int studentId, int scholarshipId, double gpa) {
        String check = """
            SELECT s.min_gpa, s.available_slots, s.deadline
            FROM Scholarships s WHERE s.scholarship_id = ?
            """;
        String duplicate = """
            SELECT COUNT(*) FROM Application
            WHERE scholarship_id = ? AND student_id = ?
            """;
        String insert = """
            INSERT INTO Application
                (scholarship_id, student_id, status, gpa_submitted, submission_date)
            VALUES (?, ?, 'PENDING', ?, ?)
            """;
        try (Connection c = DatabaseConnection.connect()) {
            try (PreparedStatement p = c.prepareStatement(check)) {
                p.setInt(1, scholarshipId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return false;
                    if (r.getInt("available_slots") <= 0
                            || gpa < r.getDouble("min_gpa")
                            || LocalDate.parse(r.getString("deadline")).isBefore(LocalDate.now())) {
                        return false;
                    }
                }
            }
            try (PreparedStatement p = c.prepareStatement(duplicate)) {
                p.setInt(1, scholarshipId);
                p.setInt(2, studentId);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next() && r.getInt(1) > 0) return false;
                }
            }
            try (PreparedStatement p = c.prepareStatement(insert)) {
                p.setInt(1, scholarshipId);
                p.setInt(2, studentId);
                p.setDouble(3, gpa);
                p.setString(4, LocalDate.now().toString());
                return p.executeUpdate() > 0;
            }
        } catch (SQLException | RuntimeException ex) {
            System.err.println("[Application] Submission failed: " + ex.getMessage());
            return false;
        }
    }

    public List<Application> getByStudent(int studentId) {
        return query("""
            SELECT a.application_id, a.scholarship_id, s.title AS scholarship_title,
                   a.student_id, u.full_name AS student_name,
                   COALESCE(a.evaluator_id, 0) AS evaluator_id, a.status,
                   a.gpa_submitted, a.submission_date
            FROM Application a
            JOIN Scholarships s ON s.scholarship_id = a.scholarship_id
            JOIN Users u ON u.user_id = a.student_id
            WHERE a.student_id = ?
            ORDER BY a.application_id DESC
            """, studentId);
    }

    public List<Application> getAll() {
        return query("""
            SELECT a.application_id, a.scholarship_id, s.title AS scholarship_title,
                   a.student_id, u.full_name AS student_name,
                   COALESCE(a.evaluator_id, 0) AS evaluator_id, a.status,
                   a.gpa_submitted, a.submission_date
            FROM Application a
            JOIN Scholarships s ON s.scholarship_id = a.scholarship_id
            JOIN Users u ON u.user_id = a.student_id
            ORDER BY a.application_id DESC
            """, null);
    }

    public boolean review(int applicationId, int adminId, String decision) {
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) return false;
        String select = """
            SELECT a.status, a.scholarship_id, s.available_slots
            FROM Application a JOIN Scholarships s
              ON s.scholarship_id = a.scholarship_id
            WHERE a.application_id = ?
            """;
        try (Connection c = DatabaseConnection.connect()) {
            c.setAutoCommit(false);
            try {
                int scholarshipId;
                try (PreparedStatement p = c.prepareStatement(select)) {
                    p.setInt(1, applicationId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next() || !"PENDING".equalsIgnoreCase(r.getString("status"))) {
                            c.rollback();
                            return false;
                        }
                        scholarshipId = r.getInt("scholarship_id");
                        if ("APPROVED".equals(decision) && r.getInt("available_slots") <= 0) {
                            c.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("""
                        UPDATE Application SET status = ?, evaluator_id = ?
                        WHERE application_id = ? AND status = 'PENDING'
                        """)) {
                    p.setString(1, decision);
                    p.setInt(2, adminId);
                    p.setInt(3, applicationId);
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return false;
                    }
                }
                if ("APPROVED".equals(decision)) {
                    try (PreparedStatement p = c.prepareStatement("""
                            UPDATE Scholarships SET available_slots = available_slots - 1
                            WHERE scholarship_id = ? AND available_slots > 0
                            """)) {
                        p.setInt(1, scholarshipId);
                        if (p.executeUpdate() != 1) {
                            c.rollback();
                            return false;
                        }
                    }
                }
                c.commit();
                return true;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            System.err.println("[Application] Review failed: " + ex.getMessage());
            return false;
        }
    }

    private List<Application> query(String sql, Integer studentId) {
        List<Application> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            if (studentId != null) p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    result.add(new Application(
                            r.getInt("application_id"),
                            r.getInt("scholarship_id"),
                            r.getString("scholarship_title"),
                            r.getInt("student_id"),
                            r.getString("student_name"),
                            r.getInt("evaluator_id"),
                            r.getString("status"),
                            r.getDouble("gpa_submitted"),
                            r.getString("submission_date")
                    ));
                }
            }
        } catch (SQLException ex) {
            System.err.println("[Application] Retrieval failed: " + ex.getMessage());
        }
        return result;
    }
}
