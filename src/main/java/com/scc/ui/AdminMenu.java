
package com.scc.ui;

import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ScholarshipDao scholarshipDao;
    private final Scanner scanner;

    public AdminMenu(
            UserDao userDao,
            ScholarshipDao scholarshipDao,
            Scanner scanner
    ) {
        this.userDao = userDao;
        this.scholarshipDao = scholarshipDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean running = true;

        while (running) {
            System.out.println("\n==========================================");
            System.out.println("              ADMIN DASHBOARD");
            System.out.println("==========================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending Users");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Logout");
            System.out.println("==========================================");
            System.out.print("Choose an option (1-8): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
                case "4" -> addNewUser(admin);
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
                case "7" -> viewAuditLogs();
                case "8" -> {
                    running = false;
                    System.out.println("Admin logged out successfully.");
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    // ==========================================
    // 1. VIEW ALL USERS
    // ==========================================

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();

        System.out.println("\n================ ALL REGISTERED USERS ================");

        printUserHeader();

        if (users.isEmpty()) {
            System.out.println("No registered users found.");
            return;
        }

        for (User user : users) {
            printUser(user);
        }

        System.out.println("Total users: " + users.size());
    }

    // ==========================================
    // 2. VIEW PENDING USERS
    // ==========================================

    private void viewPendingUsers() {
        List<User> users = userDao.getPendingUsers();

        System.out.println("\n================ PENDING USERS ================");

        printUserHeader();

        if (users.isEmpty()) {
            System.out.println("No pending registrations found.");
            return;
        }

        for (User user : users) {
            printUser(user);
        }

        System.out.println("Total pending users: " + users.size());
    }

    // ==========================================
    // 3. APPROVE USER REGISTRATION
    // ==========================================

    private void approveUser(User admin) {
        viewPendingUsers();

        System.out.print("\nEnter User ID to approve: ");
        int userId = readUserId();

        if (userId == -1) {
            return;
        }

        User user = userDao.getUserById(userId);

        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(user.status())) {
            System.out.println("This user is not pending approval.");
            return;
        }

        System.out.println("\nSelected User:");
        System.out.println("ID: " + user.userId());
        System.out.println("Name: " + user.fullName());
        System.out.println("Email: " + user.email());
        System.out.println("Role: " + user.role());
        System.out.println("Status: " + user.status());

        System.out.print("\nApprove this registration? (Y/N): ");

        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Approval cancelled.");
            return;
        }

        if (userDao.updateUserStatus(user.email(), "APPROVED")) {

            System.out.println("\nUser approved successfully!");
            System.out.println("User ID: " + user.userId());
            System.out.println("Name: " + user.fullName());
            System.out.println("New Status: APPROVED");

            userDao.addAuditLog(
                    admin.userId(),
                    "APPROVE USER",
                    "Approved account: " + user.fullName()
            );

        } else {
            System.out.println("Failed to approve user.");
        }
    }

    // ==========================================
    // 4. ADD NEW USER
    // ==========================================

    private void addNewUser(User admin) {
        System.out.println("\n========== ADD NEW USER ==========");

        System.out.print("Full Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim().toLowerCase();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.println("\nAvailable Roles:");
        System.out.println("1. STUDENT");
        System.out.println("2. ADMIN");
        System.out.print("Choose Role: ");

        String roleChoice = scanner.nextLine().trim();

        String role;

        if (roleChoice.equals("1")) {
            role = "STUDENT";
        } else if (roleChoice.equals("2")) {
            role = "ADMIN";
        } else {
            System.out.println("Invalid role.");
            return;
        }

        System.out.print("Department (leave blank if none): ");
        String department = scanner.nextLine().trim();

        System.out.println("\nAccount Status:");
        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.print("Choose Status: ");

        String statusChoice = scanner.nextLine().trim();

        String status;

        if (statusChoice.equals("1")) {
            status = "PENDING";
        } else if (statusChoice.equals("2")) {
            status = "APPROVED";
        } else {
            System.out.println("Invalid status.");
            return;
        }

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            System.out.println("Name, email, and password are required.");
            return;
        }

        if (userDao.getUserByEmail(email) != null) {
            System.out.println("Email is already registered.");
            return;
        }

        User newUser = new User(
                name,
                email,
                password,
                role,
                department.isBlank() ? null : department,
                status
        );

        if (userDao.insertUser(newUser)) {

            System.out.println("\nNew user added successfully!");
            System.out.println("Name: " + name);
            System.out.println("Email: " + email);
            System.out.println("Role: " + role);
            System.out.println("Status: " + status);

            userDao.addAuditLog(
                    admin.userId(),
                    "ADD USER",
                    "Added user: " + name + " (" + email + ")"
            );

        } else {
            System.out.println("Failed to add user.");
        }
    }

    // ==========================================
    // 5. UPDATE USER ROLE / STATUS
    // ==========================================

    private void updateUser(User admin) {
        viewAllUsers();

        System.out.print("\nEnter User ID to update: ");
        int userId = readUserId();

        if (userId == -1) {
            return;
        }

        User user = userDao.getUserById(userId);

        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        if (user.userId() == admin.userId()) {
            System.out.println("You cannot modify your own admin account.");
            return;
        }

        System.out.println("\n========== UPDATE USER ==========");
        System.out.println("User ID: " + user.userId());
        System.out.println("Name: " + user.fullName());
        System.out.println("Current Role: " + user.role());
        System.out.println("Current Status: " + user.status());

        System.out.println("\nAvailable Roles:");
        System.out.println("1. STUDENT");
        System.out.println("2. ADMIN");
        System.out.print("Choose New Role: ");

        String roleChoice = scanner.nextLine().trim();

        String newRole;

        if (roleChoice.equals("1")) {
            newRole = "STUDENT";
        } else if (roleChoice.equals("2")) {
            newRole = "ADMIN";
        } else {
            System.out.println("Invalid role.");
            return;
        }

        System.out.println("\nAvailable Status:");
        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.print("Choose New Status: ");

        String statusChoice = scanner.nextLine().trim();

        String newStatus;

        if (statusChoice.equals("1")) {
            newStatus = "PENDING";
        } else if (statusChoice.equals("2")) {
            newStatus = "APPROVED";
        } else {
            System.out.println("Invalid status.");
            return;
        }

        System.out.println("\nNew Role: " + newRole);
        System.out.println("New Status: " + newStatus);

        System.out.print("Confirm update? (Y/N): ");

        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(
                userId,
                newRole,
                newStatus
        )) {

            System.out.println("User updated successfully!");

            userDao.addAuditLog(
                    admin.userId(),
                    "UPDATE USER",
                    "Updated " + user.fullName()
                            + ": Role=" + newRole
                            + ", Status=" + newStatus
            );

        } else {
            System.out.println("Failed to update user.");
        }
    }

    // ==========================================
    // 6. DELETE USER ACCOUNT
    // ==========================================

    private void deleteUser(User admin) {
        viewAllUsers();

        System.out.print("\nEnter User ID to delete: ");
        int userId = readUserId();

        if (userId == -1) {
            return;
        }

        User user = userDao.getUserById(userId);

        if (user == null) {
            System.out.println("User not found.");
            return;
        }

        if (user.userId() == admin.userId()) {
            System.out.println("You cannot delete your own admin account.");
            return;
        }

        System.out.println("\nSelected Account:");
        System.out.println("ID: " + user.userId());
        System.out.println("Name: " + user.fullName());
        System.out.println("Email: " + user.email());

        System.out.print("\nAre you sure you want to delete this user? (Y/N): ");

        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        if (userDao.deleteUserById(userId)) {

            System.out.println("User account deleted successfully!");

            userDao.addAuditLog(
                    admin.userId(),
                    "DELETE USER",
                    "Deleted account: " + user.fullName()
                            + " (" + user.email() + ")"
            );

        } else {
            System.out.println(
                    "Unable to delete user. This account may have related records."
            );
        }
    }

    // ==========================================
    // 7. VIEW SYSTEM AUDIT LOGS
    // ==========================================

    private void viewAuditLogs() {
        List<String> logs = userDao.getAuditLogs();

        System.out.println("\n================ SYSTEM AUDIT LOGS ================");

        if (logs.isEmpty()) {
            System.out.println("No audit logs available.");
            return;
        }

        for (String log : logs) {
            System.out.println(log);
        }

        System.out.println("\nTotal audit logs: " + logs.size());
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void printUserHeader() {
        System.out.printf(
                "%-6s %-22s %-28s %-12s %-12s%n",
                "ID",
                "NAME",
                "EMAIL",
                "ROLE",
                "STATUS"
        );

        System.out.println(
                "--------------------------------------------------------------------------------"
        );
    }

    private void printUser(User user) {
        System.out.printf(
                "%-6d %-22s %-28s %-12s %-12s%n",
                user.userId(),
                user.fullName(),
                user.email(),
                user.role(),
                user.status()
        );
    }

    private int readUserId() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid User ID.");
            return -1;
        }
    }
}
