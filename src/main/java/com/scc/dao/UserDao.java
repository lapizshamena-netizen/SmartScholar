
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

    // ======================================
    // INSERT USER
    // ======================================

    public boolean insertUser(User user) {

        String sql = """
            INSERT INTO Users
                (full_name, email, password_hash,
                 role, department, status)
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
                            : user.status().toUpperCase()
            );

            pstmt.executeUpdate();

            return true;

        } catch (SQLException e) {

            System.err.println(
                    "Could not register user: " + e.getMessage()
            );

            return false;
        }
    }

    // ======================================
    // GET USER BY EMAIL
    // ======================================

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

            System.err.println(
                    "Error retrieving user: " + e.getMessage()
            );
        }

        return null;
    }

    // ======================================
    // GET USER BY ID
    // ======================================

    public User getUserById(int userId) {

        String sql = """
            SELECT user_id, full_name, email, password_hash,
                   role, department, status
            FROM Users
            WHERE user_id = ?
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapUser(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "Error retrieving user: " + e.getMessage()
            );
        }

        return null;
    }

    // ======================================
    // GET ALL USERS
    // ======================================

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

            System.err.println(
                    "Error retrieving users: " + e.getMessage()
            );
        }

        return users;
    }

    // ======================================
    // GET PENDING USERS
    // ======================================

    public List<User> getPendingUsers() {

        List<User> users = new ArrayList<>();

        String sql = """
            SELECT user_id, full_name, email, password_hash,
                   role, department, status
            FROM Users
            WHERE UPPER(status) = 'PENDING'
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

            System.err.println(
                    "Error retrieving pending users: "
                            + e.getMessage()
            );
        }

        return users;
    }

    // ======================================
    // APPROVE / UPDATE USER STATUS
    // ======================================

    public boolean updateUserStatus(
            String email,
            String status
    ) {

        if (!"PENDING".equalsIgnoreCase(status)
                && !"APPROVED".equalsIgnoreCase(status)) {
            return false;
        }

        String sql = """
            UPDATE Users
            SET status = ?
            WHERE email = ?
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, status.toUpperCase());
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error updating user status: " + e.getMessage()
            );

            return false;
        }
    }

    // ======================================
    // UPDATE USER ROLE AND STATUS
    // ======================================

    public boolean updateUserRoleAndStatus(
            int userId,
            String role,
            String status
    ) {

        if (!"STUDENT".equalsIgnoreCase(role)
                && !"ADMIN".equalsIgnoreCase(role)) {
            return false;
        }

        if (!"PENDING".equalsIgnoreCase(status)
                && !"APPROVED".equalsIgnoreCase(status)) {
            return false;
        }

        String sql = """
            UPDATE Users
            SET role = ?, status = ?
            WHERE user_id = ?
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, role.toUpperCase());
            pstmt.setString(2, status.toUpperCase());
            pstmt.setInt(3, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error updating user: " + e.getMessage()
            );

            return false;
        }
    }

    // ======================================
    // DELETE USER
    // ======================================

    public boolean deleteUserById(int userId) {

        String sql = """
            DELETE FROM Users
            WHERE user_id = ?
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "Error deleting user: " + e.getMessage()
            );

            return false;
        }
    }

    // ======================================
    // MAP RESULT TO USER
    // ======================================

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
