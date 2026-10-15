package com.scc.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:smart_scholar.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
        return conn;
    }

    public static void createTables() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS Users ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "full_name TEXT NOT NULL, "
                + "email TEXT NOT NULL, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "department TEXT"
                + ");";

        String sqlScholarships = "CREATE TABLE IF NOT EXISTS Scholarships ("
                + "scholarship_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL, "
                + "description TEXT, "
                + "grant_amount REAL, "
                + "min_gpa REAL, "
                + "available_slots INTEGER, "
                + "deadline TEXT, "
                + "created_by INTEGER, "
                + "FOREIGN KEY (created_by) REFERENCES Users(user_id)"
                + ");";

        String sqlApplication = "CREATE TABLE IF NOT EXISTS Application ("
                + "application_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "scholarship_id INTEGER, "
                + "student_id INTEGER, "
                + "evaluator_id INTEGER, "
                + "status TEXT DEFAULT 'Pending', "
                + "gpa_submitted REAL, "
                + "submission_date TEXT, "
                + "FOREIGN KEY (scholarship_id) REFERENCES Scholarships(scholarship_id), "
                + "FOREIGN KEY (student_id) REFERENCES Users(user_id), "
                + "FOREIGN KEY (evaluator_id) REFERENCES Users(user_id)"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sqlUsers);
                stmt.execute(sqlScholarships);
                stmt.execute(sqlApplication);
            }
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }
}