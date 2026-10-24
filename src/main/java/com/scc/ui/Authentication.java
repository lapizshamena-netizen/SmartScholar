
package com.scc.ui;

import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ScholarshipDao scholarshipDao;
    private final Scanner scanner;

    public Authentication(
            UserDao userDao,
            ScholarshipDao scholarshipDao,
            Scanner scanner
    ) {
        this.userDao = userDao;
        this.scholarshipDao = scholarshipDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n==========================================");
            System.out.println("       Welcome to SmartScholar CLI");
            System.out.println("==========================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose (1-3): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "3" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void register() {
        System.out.println("\n--- REGISTER ACCOUNT ---");

        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim().toLowerCase();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.println("Available roles: STUDENT, ADMIN");
        System.out.print("Role: ");
        String role = scanner.nextLine().trim().toUpperCase();

        if (!role.equals("STUDENT") && !role.equals("ADMIN")) {
            System.out.println(
                    "Invalid role. Choose STUDENT or ADMIN."
            );
            return;
        }

        System.out.print("Department (or leave blank): ");
        String department = scanner.nextLine().trim();

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println(
                    "Name, email, and password are required."
            );
            return;
        }

        // New accounts always start as PENDING.
        User user = new User(
                name,
                email,
                password,
                role,
                department.isBlank() ? null : department,
                "PENDING"
        );

        if (userDao.insertUser(user)) {
            System.out.println("\nRegistration successful!");
            System.out.println("Account status: PENDING");
            System.out.println(
                    "Please wait for administrator approval."
            );
        }
    }

    private void login() {
        System.out.println("\n--- LOGIN ---");

        System.out.print("Email: ");
        String email = scanner.nextLine().trim().toLowerCase();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = userDao.getUserByEmail(email);

        if (user == null || !user.passwordHash().equals(password)) {
            System.out.println("Invalid email or password.");
            return;
        }

        if ("PENDING".equalsIgnoreCase(user.status())) {
            System.out.println(
                    "Your account is PENDING approval."
            );
            System.out.println(
                    "Please contact an administrator."
            );
            return;
        }

        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println(
                    "Your account is not approved."
            );
            return;
        }

        System.out.println(
                "\nLogin successful! Welcome, " +
                        user.fullName() + " (" + user.role() + ")"
        );

        routeByRole(user);
    }

    private void routeByRole(User user) {
        switch (user.role().toUpperCase()) {
            case "ADMIN" ->
                    new AdminMenu(
                            userDao,
                            scholarshipDao,
                            scanner
                    ).display(user);

            case "STUDENT" ->
                    new RoleMenu(
                            scanner,
                            "STUDENT"
                    ).display(user);

            default ->
                    System.out.println(
                            "No menu is configured for role: " +
                                    user.role()
                    );
        }
    }
}
