
package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ScholarshipDao scholarshipDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public AdminMenu(
            UserDao userDao,
            ScholarshipDao scholarshipDao,
            Scanner scanner
    ) {

        this.userDao = userDao;
        this.scholarshipDao = scholarshipDao;
        this.scanner = scanner;
        this.activityLogDao = new ActivityLogDao();
    }

    // ======================================
    // ADMIN DASHBOARD
    // ======================================

    public void display(User admin) {

        boolean running = true;

        while (running) {

            System.out.println("\n==========================================");
            System.out.println("             ADMIN DASHBOARD");
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

                default -> System.out.println("Invalid option.");
            }
        }
    }

    // ======================================
    // DISPLAY ALL USERS
    // ======================================

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

        System.out.println("\nTotal users: " + users.size());
    }

    // ======================================
    // DISPLAY PENDING USERS
    // ======================================

    private void viewPendingUsers() {

        List<User> users = userDao.getPendingUsers();

        System.out.println("\n================ PENDING USERS ================");

        printUserHeader();

        if (users.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }

        for (User user : users) {
            printUser(user);
        }

        System.out.println("\nTotal pending users: " + users.size());
    }

    // ======================================
    // APPROVE USER REGISTRATION
    // ======================================

    private void approveUser(User admin) {

        List<User> pendingUsers = userDao.getPendingUsers();

        System.out.println("\n========== APPROVE USER REGISTRATION ==========");

        if (pendingUsers.isEmpty()) {
            System.out.println("There are no pending users.");
            return;
        }

        printUserHeader();

        for (User user : pendingUsers) {
            printUser(user);
        }

        System.out.print("\nEnter User ID to approve: ");

        int userId = readPositiveId();

        if (userId == -1) {
            return;
        }

        User selected = userDao.getUserById(userId);

        if (selected == null) {
            System.out.println("User ID not found.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selected.status())) {
            System.out.println("This account is not pending.");
            return;
        }

        System.out.println("\nSelected Account:");
        System.out.println("ID: " + selected.userId());
        System.out.println("Name: " + selected.fullName());
        System.out.println("Email: " + selected.email());
        System.out.println("Role: " + selected.role());

        System.out.print("\nApprove this account? (Y/N): ");

        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Approval cancelled.");
            return;
        }

        if (userDao.updateUserStatus(
                selected.email(),
                "APPROVED"
        )) {

            boolean logged = activityLogDao.logAction(
                    admin.userId(),
                    "USER_APPROVED",
                    "Admin ID " + admin.userId()
                            + " approved User ID " + selected.userId()
                            + " (" + selected.fullName() + ")"
            );

            System.out.println(
                    selected.fullName()
                            + " has been APPROVED successfully."
            );

            if (!logged) {
                System.out.println(
                        "Warning: Approval succeeded but audit logging failed."
                );
            }

        } else {

            System.out.println("Failed to approve user.");
        }
    }

    // ======================================
    // ADD NEW USER
    // ======================================

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

        switch (roleChoice) {
            case "1" -> role = "STUDENT";
            case "2" -> role = "ADMIN";
            default -> {
                System.out.println("Invalid role.");
                return;
            }
        }

        System.out.print("Department: ");
        String department = scanner.nextLine().trim();

        System.out.println("\nAccount Status:");
        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.print("Choose Status: ");

        String statusChoice = scanner.nextLine().trim();

        String status;

        switch (statusChoice) {
            case "1" -> status = "PENDING";
            case "2" -> status = "APPROVED";
            default -> {
                System.out.println("Invalid status.");
                return;
            }
        }

        if (name.isBlank()
                || email.isBlank()
                || password.isBlank()) {

            System.out.println("All required fields must be completed.");
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

            User insertedUser = userDao.getUserByEmail(email);

            if (insertedUser != null) {

                boolean logged = activityLogDao.logAction(
                        admin.userId(),
                        "USER_ADDED",
                        "Admin ID " + admin.userId()
                                + " added User ID "
                                + insertedUser.userId()
                                + " (" + name + ")"
                );

                if (!logged) {
                    System.out.println("Warning: Audit log failed.");
                }
            }

            System.out.println("\nUser added successfully!");
            System.out.println("Name: " + name);
            System.out.println("Role: " + role);
            System.out.println("Status: " + status);

        } else {

            System.out.println("Failed to add user.");
        }
    }

    // ======================================
    // UPDATE USER ROLE / STATUS
    // ======================================

    private void updateUser(User admin) {

        System.out.println("\n========== UPDATE USER ROLE / STATUS ==========");

        viewAllUsers();

        System.out.print("\nEnter User ID to update: ");

        int userId = readPositiveId();

        if (userId == -1) {
            return;
        }

        User selected = userDao.getUserById(userId);

        if (selected == null) {
            System.out.println("User not found.");
            return;
        }

        System.out.println("\nSelected User:");
        System.out.println("ID: " + selected.userId());
        System.out.println("Name: " + selected.fullName());
        System.out.println("Current Role: " + selected.role());
        System.out.println("Current Status: " + selected.status());

        System.out.println("\nAvailable Roles:");
        System.out.println("1. STUDENT");
        System.out.println("2. ADMIN");
        System.out.print("New Role: ");

        String roleChoice = scanner.nextLine().trim();

        String newRole;

        switch (roleChoice) {
            case "1" -> newRole = "STUDENT";
            case "2" -> newRole = "ADMIN";
            default -> {
                System.out.println("Invalid role.");
                return;
            }
        }

        System.out.println("\nAvailable Status:");
        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.print("New Status: ");

        String statusChoice = scanner.nextLine().trim();

        String newStatus;

        switch (statusChoice) {
            case "1" -> newStatus = "PENDING";
            case "2" -> newStatus = "APPROVED";
            default -> {
                System.out.println("Invalid status.");
                return;
            }
        }

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

            boolean logged = activityLogDao.logAction(
                    admin.userId(),
                    "USER_UPDATED",
                    "Admin ID " + admin.userId()
                            + " updated User ID " + userId
                            + " role from " + selected.role()
                            + " to " + newRole
                            + " and status from " + selected.status()
                            + " to " + newStatus
            );

            System.out.println("User updated successfully!");

            if (!logged) {
                System.out.println("Warning: Audit log failed.");
            }

        } else {

            System.out.println("Failed to update user.");
        }
    }

    // ======================================
    // DELETE USER ACCOUNT
    // ======================================

    private void deleteUser(User admin) {

        System.out.println("\n========== DELETE USER ACCOUNT ==========");

        viewAllUsers();

        System.out.print("\nEnter User ID to delete: ");

        int userId = readPositiveId();

        if (userId == -1) {
            return;
        }

        if (userId == admin.userId()) {
            System.out.println("You cannot delete your own account.");
            return;
        }

        User selected = userDao.getUserById(userId);

        if (selected == null) {
            System.out.println("User not found.");
            return;
        }

        System.out.println("\nAccount to delete:");
        System.out.println("ID: " + selected.userId());
        System.out.println("Name: " + selected.fullName());
        System.out.println("Email: " + selected.email());

        System.out.print(
                "\nAre you sure you want to delete this account? (Y/N): "
        );

        if (!scanner.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.println("Deletion cancelled.");
            return;
        }

        // Log BEFORE deletion so the audit record is retained
        // even if the account is deleted successfully.
        if (userDao.deleteUserById(userId)) {

            boolean logged = activityLogDao.logAction(
                    admin.userId(),
                    "USER_DELETED",
                    "Admin ID " + admin.userId()
                            + " deleted User ID " + userId
                            + " (" + selected.fullName() + ")"
            );

            System.out.println("User account deleted successfully.");

            if (!logged) {
                System.out.println("Warning: Audit log failed.");
            }

        } else {

            System.out.println(
                    "Could not delete account. "
                            + "The user may have related records."
            );
        }
    }

    // ======================================
    // SYSTEM AUDIT LOGS
    // ======================================

    private void viewAuditLogs() {

        boolean running = true;

        while (running) {

            System.out.println("\n========== SYSTEM AUDIT LOGS ==========");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("4. Back to Admin Dashboard");
            System.out.print("Choose (1-4): ");

            switch (scanner.nextLine().trim()) {

                case "1" -> displayAllLogs();

                case "2" -> filterLogsByUserId();

                case "3" -> clearHistoricalLogs();

                case "4" -> running = false;

                default -> System.out.println("Invalid option.");
            }
        }
    }

    // ======================================
    // VIEW ALL LOGS
    // ======================================

    private void displayAllLogs() {

        List<ActivityLog> logs = activityLogDao.getAllLogs();

        System.out.println("\n================ SYSTEM AUDIT LOGS ================");

        printLogHeader();

        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        for (ActivityLog log : logs) {
            printLog(log);
        }

        System.out.println("\nTotal logs: " + logs.size());
    }

    // ======================================
    // FILTER LOGS BY USER ID
    // ======================================

    private void filterLogsByUserId() {

        System.out.print("\nEnter User ID: ");

        int userId = readPositiveId();

        if (userId == -1) {
            return;
        }

        List<ActivityLog> logs =
                activityLogDao.getLogsByUserId(userId);

        System.out.println(
                "\n========== LOGS FOR USER ID "
                        + userId + " =========="
        );

        printLogHeader();

        if (logs.isEmpty()) {
            System.out.println("No logs found for this User ID.");
            return;
        }

        for (ActivityLog log : logs) {
            printLog(log);
        }

        System.out.println("\nTotal logs: " + logs.size());
    }

    // ======================================
    // CLEAR HISTORICAL LOGS
    // ======================================

    private void clearHistoricalLogs() {

        System.out.println(
                "\nWARNING: This permanently deletes all historical logs."
        );

        System.out.print("Type YES to confirm: ");

        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equals("YES")) {
            System.out.println("Clear operation cancelled.");
            return;
        }

        if (activityLogDao.clearAllLogs()) {
            System.out.println("All historical logs cleared successfully.");
        } else {
            System.out.println("Failed to clear historical logs.");
        }
    }

    // ======================================
    // LOG TABLE HEADER
    // ======================================

    private void printLogHeader() {

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "ID",
                "USER ID",
                "ACTION TYPE",
                "DESCRIPTION",
                "TIMESTAMP"
        );

        System.out.println(
                "---------------------------------------------------------------------------------------------------------------"
        );
    }

    // ======================================
    // PRINT INDIVIDUAL LOG
    // ======================================

    private void printLog(ActivityLog log) {

        System.out.printf(
                "%-5d | %-8d | %-18s | %-35s | %-20s%n",
                log.logId(),
                log.userId(),
                shorten(log.actionType(), 18),
                shorten(log.description(), 35),
                log.timestamp()
        );
    }

    // ======================================
    // USER TABLE HEADER
    // ======================================

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
                "------------------------------------------------------------------------------------------"
        );
    }

    // ======================================
    // PRINT INDIVIDUAL USER
    // ======================================

    private void printUser(User user) {

        System.out.printf(
                "%-6d %-22s %-28s %-12s %-12s%n",
                user.userId(),
                shorten(user.fullName(), 22),
                shorten(user.email(), 28),
                user.role(),
                user.status()
        );
    }

    // ======================================
    // SHORTEN LONG TEXT
    // ======================================

    private String shorten(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }

    // ======================================
    // READ AND VALIDATE USER ID
    // ======================================

    private int readPositiveId() {

        String input = scanner.nextLine().trim();

        try {

            int id = Integer.parseInt(input);

            if (id <= 0) {
                System.out.println("ID must be greater than zero.");
                return -1;
            }

            return id;

        } catch (NumberFormatException e) {

            System.out.println("Invalid ID. Please enter a number.");
            return -1;
        }
    }
}
