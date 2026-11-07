
package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {

        try (
                Connection connection = DatabaseConnection.connect();
                Statement statement = connection.createStatement()
        ) {

            // ======================================
            // USERS TABLE
            // ======================================

            statement.execute("""
                CREATE TABLE IF NOT EXISTS Users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    department TEXT,
                    status TEXT NOT NULL DEFAULT 'PENDING'
                )
                """);

            // Add status to an older Users table.
            if (!hasColumn(connection, "Users", "status")) {

                statement.execute("""
                    ALTER TABLE Users
                    ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'
                    """);

                System.out.println(
                        "[Database] Added Users.status column."
                );
            }

            // ======================================
            // SCHOLARSHIPS TABLE
            // ======================================

            statement.execute("""
                CREATE TABLE IF NOT EXISTS Scholarships (
                    scholarship_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    grant_amount REAL NOT NULL,
                    min_gpa REAL NOT NULL,
                    available_slots INTEGER NOT NULL,
                    deadline TEXT NOT NULL,
                    created_by INTEGER,
                    FOREIGN KEY (created_by)
                        REFERENCES Users(user_id)
                )
                """);

            // ======================================
            // APPLICATION TABLE
            // ======================================

            statement.execute("""
                CREATE TABLE IF NOT EXISTS Application (
                    application_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scholarship_id INTEGER,
                    student_id INTEGER,
                    evaluator_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    gpa_submitted REAL,
                    submission_date TEXT,
                    FOREIGN KEY (scholarship_id)
                        REFERENCES Scholarships(scholarship_id),
                    FOREIGN KEY (student_id)
                        REFERENCES Users(user_id),
                    FOREIGN KEY (evaluator_id)
                        REFERENCES Users(user_id)
                )
                """);

            // ======================================
            // ACTIVITY LOGS TABLE
            // ======================================

            statement.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

            // Index for faster user ID filtering.
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id
                ON activity_logs(user_id)
                """);

            System.out.println(
                    "[Database] Users table initialized."
            );

            System.out.println(
                    "[Database] Scholarships table initialized."
            );

            System.out.println(
                    "[Database] Application table initialized."
            );

            System.out.println(
                    "[Database] Activity logs table initialized."
            );

            System.out.println(
                    "[Database] All tables initialized successfully."
            );
        }
    }

    // ======================================
    // CHECK IF A COLUMN EXISTS
    // ======================================

    private static boolean hasColumn(
            Connection connection,
            String table,
            String column
    ) throws SQLException {

        try (
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "PRAGMA table_info('" + table + "')"
                )
        ) {

            while (rs.next()) {

                if (column.equalsIgnoreCase(
                        rs.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
