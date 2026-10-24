package com.scc.dao;

import com.scc.model.Scholarship;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScholarshipDao {

    public boolean insertScholarship(Scholarship s) {
        String sql = """
            INSERT INTO Scholarships
                (title, description, grant_amount, min_gpa,
                 available_slots, deadline, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, s.title());
            pstmt.setString(2, s.description());
            pstmt.setDouble(3, s.grantAmount());
            pstmt.setDouble(4, s.minGpa());
            pstmt.setInt(5, s.availableSlots());
            pstmt.setString(6, s.deadline());

            if (s.createdBy() > 0) {
                pstmt.setInt(7, s.createdBy());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println(
                    "Could not create scholarship: " + e.getMessage()
            );
            return false;
        }
    }

    public List<Scholarship> getAllScholarships() {
        List<Scholarship> list = new ArrayList<>();

        String sql = """
            SELECT * FROM Scholarships
            ORDER BY scholarship_id
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                list.add(new Scholarship(
                        rs.getInt("scholarship_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("grant_amount"),
                        rs.getDouble("min_gpa"),
                        rs.getInt("available_slots"),
                        rs.getString("deadline"),
                        rs.getInt("created_by")
                ));
            }

        } catch (SQLException e) {
            System.err.println(
                    "Could not retrieve scholarships: " + e.getMessage()
            );
        }

        return list;
    }
}