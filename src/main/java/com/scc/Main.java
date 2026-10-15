package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final UserDao userDao = new UserDao();

    public static void main(String[] args) {
        // Initialize tables on startup
        DatabaseConnection.createTables();

        boolean running = true;
        while (running) {
            System.out.println("\n=================================");
            System.out.println("      SMARTSCHOLAR SYSTEM        ");
            System.out.println("=================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine();

            switch (choice) {
                case "1":
                    System.out.println("\n[Login feature coming soon!]");
                    break;
                case "2":
                    registerUser();
                    break;
                case "3":
                    running = false;
                    System.out.println("Exiting SmartScholar. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void registerUser() {
        System.out.println("\n--- User Registration ---");
        System.out.print("Enter full name: ");
        String fullName = sc.nextLine();

        System.out.print("Enter email: ");
        String email = sc.nextLine();

        System.out.print("Enter password: ");
        String password = sc.nextLine(); // In production, hash this password!

        System.out.print("Enter role (Student / Evaluator / Admin): ");
        String role = sc.nextLine();

        System.out.print("Enter department: ");
        String department = sc.nextLine();

        User newUser = new User(fullName, email, password, role, department);
        boolean success = userDao.insertUser(newUser);

        if (success) {
            System.out.println("Registration successful!");
        } else {
            System.out.println("Registration failed.");
        }
    }
}