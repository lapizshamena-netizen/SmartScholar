
package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // Insert a new activity log.
    public boolean logAction(
            int userId,
            String actionType,
            String description
    ) {

        String sql = """
            INSERT INTO activity_logs
                (user_id, action_type, description)
            VALUES (?, ?, ?)
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "[Audit Log] Insert failed: " + e.getMessage()
            );

            return false;
        }
    }

    // Retrieve all logs.
    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
            SELECT log_id, user_id, action_type,
                   description, timestamp
            FROM activity_logs
            ORDER BY timestamp DESC, log_id DESC
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {

            while (rs.next()) {
                logs.add(mapLog(rs));
            }

        } catch (SQLException e) {

            System.err.println(
                    "[Audit Log] Retrieval failed: " + e.getMessage()
            );
        }

        return logs;
    }

    // Retrieve logs belonging to a particular user.
    public List<ActivityLog> getLogsByUserId(int userId) {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
            SELECT log_id, user_id, action_type,
                   description, timestamp
            FROM activity_logs
            WHERE user_id = ?
            ORDER BY timestamp DESC, log_id DESC
            """;

        try (
                Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    logs.add(mapLog(rs));
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "[Audit Log] Filter failed: " + e.getMessage()
            );
        }

        return logs;
    }

    // Delete all historical logs.
    public boolean clearAllLogs() {

        String sql = "DELETE FROM activity_logs";

        try (
                Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement()
        ) {

            stmt.executeUpdate(sql);
            return true;

        } catch (SQLException e) {

            System.err.println(
                    "[Audit Log] Clear failed: " + e.getMessage()
            );

            return false;
        }
    }

    // Convert database result into ActivityLog.
    private ActivityLog mapLog(ResultSet rs)
            throws SQLException {

        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}
