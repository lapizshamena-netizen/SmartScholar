
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean insertUser(User user) {
        String sql = """
            INSERT INTO Users
                (full_name, email, password_hash, role, department, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, user.fullName());
            pstmt.setString(2, user.email());
            pstmt.setString(3, user.passwordHash());
            pstmt.setString(4, user.role());
            pstmt.setString(5, user.department());
            pstmt.setString(
                    6,
                    user.status() == null || user.status().isBlank()
                            ? "PENDING"
                            : user.status()
            );

            pstmt.executeUpdate();
            System.out.println("User inserted successfully!");
            return true;

        } catch (SQLException e) {
            System.out.println(
                    "Could not register user: " + e.getMessage()
            );
            return false;
        }
    }

    public User getUserByEmail(String email) {
        String sql = """
            SELECT user_id, full_name, email, password_hash,
                   role, department, status
            FROM Users
            WHERE email = ?
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving user: " + e.getMessage()
            );
        }

        return null;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        String sql = """
            SELECT user_id, full_name, email, password_hash,
                   role, department, status
            FROM Users
            ORDER BY user_id
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error retrieving users: " + e.getMessage()
            );
        }

        return users;
    }

    public boolean updateUserStatus(String email, String status) {
        if (!"PENDING".equalsIgnoreCase(status)
                && !"APPROVED".equalsIgnoreCase(status)) {
            return false;
        }

        String sql = "UPDATE Users SET status = ? WHERE email = ?";

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, status.toUpperCase());
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println(
                    "Error updating user status: " + e.getMessage()
            );
            return false;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        String status = rs.getString("status");

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("department"),
                status
        );
    }
}
