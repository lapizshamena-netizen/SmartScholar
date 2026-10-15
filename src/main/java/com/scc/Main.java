package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.AdminMenu;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();
    private static final ScholarshipDao scholarshipDao = new ScholarshipDao();

    public static void main(String[] args) {
        // Initialize SQLite database tables automatically on startup
        initializeDatabase();

        System.out.println("==========================================");
        System.out.println("       Welcome to SmartScholar CLI        ");
        System.out.println("==========================================");

        handleLogin();

        scanner.close();
    }

    private static void handleLogin() {
        System.out.print("Enter your email to login: ");
        String email = scanner.nextLine().trim();

        User user = userDao.getUserByEmail(email);

        if (user != null) {
            System.out.println("\nLogin successful! Welcome back, " + user.fullName() + " (" + user.role() + ")");

            // Route based on role
            if ("ADMIN".equalsIgnoreCase(user.role())) {
                AdminMenu adminMenu = new AdminMenu(userDao, scholarshipDao, scanner);
                adminMenu.display(user);
            } else {
                System.out.println("Student dashboard coming soon!");
            }
        } else {
            System.out.println("\nUser not found with email: " + email);
            System.out.println("Tip: Make sure you have inserted a user into your 'Users' table in SQLite.");
        }
    }

    private static void initializeDatabase() {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS Users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                department TEXT
            );
        """;

        String scholarshipsTable = """
            CREATE TABLE IF NOT EXISTS scholarships (
                scholarship_id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                grant_amount REAL NOT NULL,
                min_gpa REAL NOT NULL,
                available_slots INTEGER NOT NULL,
                deadline TEXT NOT NULL,
                created_by INTEGER,
                FOREIGN KEY (created_by) REFERENCES Users(user_id)
            );
        """;

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(scholarshipsTable);
            System.out.println("[Database] SQLite tables initialized successfully.");
        } catch (Exception e) {
            System.err.println("[Database] Error initializing tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}