package com.scc.dao;

import com.scc.model.Scholarship;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScholarshipDao {
    public boolean insertScholarship(Scholarship s) {
        String sql = """
            INSERT INTO Scholarships
                (title, description, grant_amount, min_gpa, available_slots, deadline, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection c = DatabaseConnection.connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, s.title());
            p.setString(2, s.description());
            p.setDouble(3, s.grantAmount());
            p.setDouble(4, s.minGpa());
            p.setInt(5, s.availableSlots());
            p.setString(6, s.deadline());
            if (s.createdBy() > 0) p.setInt(7, s.createdBy());
            else p.setNull(7, Types.INTEGER);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Could not create scholarship: " + e.getMessage());
            return false;
        }
    }

    public List<Scholarship> getAllScholarships() {
        List<Scholarship> list = new ArrayList<>();
        String sql = "SELECT * FROM Scholarships ORDER BY scholarship_id";
        try (Connection c = DatabaseConnection.connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {
            while (r.next()) list.add(map(r));
        } catch (SQLException e) {
            System.err.println("Could not retrieve scholarships: " + e.getMessage());
        }
        return list;
    }

    public Scholarship getById(int id) {
        String sql = "SELECT * FROM Scholarships WHERE scholarship_id = ?";
        try (Connection c = DatabaseConnection.connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return map(r);
            }
        } catch (SQLException e) {
            System.err.println("Could not retrieve scholarship: " + e.getMessage());
        }
        return null;
    }

    public boolean updateScholarship(Scholarship s) {
        String sql = """
            UPDATE Scholarships SET title=?, description=?, grant_amount=?,
                min_gpa=?, available_slots=?, deadline=? WHERE scholarship_id=?
            """;
        try (Connection c = DatabaseConnection.connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, s.title());
            p.setString(2, s.description());
            p.setDouble(3, s.grantAmount());
            p.setDouble(4, s.minGpa());
            p.setInt(5, s.availableSlots());
            p.setString(6, s.deadline());
            p.setInt(7, s.scholarshipId());
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Could not update scholarship: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteScholarship(int id) {
        String sql = "DELETE FROM Scholarships WHERE scholarship_id = ?";
        try (Connection c = DatabaseConnection.connect();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            return p.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Could not delete scholarship. It may have applications: " + e.getMessage());
            return false;
        }
    }

    private Scholarship map(ResultSet r) throws SQLException {
        return new Scholarship(
                r.getInt("scholarship_id"), r.getString("title"),
                r.getString("description"), r.getDouble("grant_amount"),
                r.getDouble("min_gpa"), r.getInt("available_slots"),
                r.getString("deadline"), r.getInt("created_by"));
    }
}
