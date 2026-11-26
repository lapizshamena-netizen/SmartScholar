package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.ApplicationDao;
import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.util.PasswordUtil;
import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final ScholarshipDao scholarshipDao;
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final Scanner scanner;

    public Authentication(UserDao userDao, ScholarshipDao scholarshipDao, Scanner scanner) {
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
            System.out.println("1. Login\n2. Register\n3. Exit");
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
        System.out.println("\n========== REGISTER ACCOUNT ==========");
        System.out.print("Full name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim().toLowerCase();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Role (STUDENT/ADMIN): ");
        String role = scanner.nextLine().trim().toUpperCase();
        System.out.print("Department (optional): ");
        String department = scanner.nextLine().trim();

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println("Name, email, and password are required."); return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            System.out.println("Enter a valid email address."); return;
        }
        if (!role.equals("STUDENT") && !role.equals("ADMIN")) {
            System.out.println("Role must be STUDENT or ADMIN."); return;
        }
        if (password.length() < 8) {
            System.out.println("Password must contain at least 8 characters."); return;
        }
        if (userDao.getUserByEmail(email) != null) {
            System.out.println("Email is already registered."); return;
        }

        User user = new User(name, email, PasswordUtil.hash(password), role,
                department.isBlank() ? null : department, "PENDING");
        if (!userDao.insertUser(user)) {
            System.out.println("Registration failed."); return;
        }
        User created = userDao.getUserByEmail(email);
        if (created != null) {
            boolean logged = activityLogDao.logAction(created.userId(), "USER_REGISTERED",
                    "New account registered: " + created.fullName() + " (" + created.email() + ")");
            if (!logged) System.out.println("Warning: Activity log could not be saved.");
        }
        System.out.println("Registration successful. Account status: PENDING.");
        System.out.println("Please wait for administrator approval.");
    }

    private void login() {
        System.out.println("\n========== LOGIN ==========");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim().toLowerCase();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        User user = userDao.getUserByEmail(email);

        if (user == null || !PasswordUtil.verify(password, user.passwordHash())) {
            System.out.println("Invalid email or password."); return;
        }
        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            System.out.println("PENDING".equalsIgnoreCase(user.status())
                    ? "Your account is pending administrator approval."
                    : "Your account is not approved.");
            return;
        }
        System.out.println("\nLogin successful! Welcome, " + user.fullName()
                + " (" + user.role() + ")");
        routeByRole(user);
    }

    private void routeByRole(User user) {
        switch (user.role().toUpperCase()) {
            case "ADMIN" -> new AdminMenu(userDao, scholarshipDao, scanner).display(user);
            case "STUDENT" -> new RoleMenu(scanner, "STUDENT", scholarshipDao,
                    applicationDao, activityLogDao).display(user);
            default -> System.out.println("No menu is configured for role: " + user.role());
        }
    }
}
