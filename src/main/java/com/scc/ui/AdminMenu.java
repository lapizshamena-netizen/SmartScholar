package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.ApplicationDao;
import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final ScholarshipDao scholarshipDao;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, ScholarshipDao scholarshipDao, Scanner scanner) {
        this.userDao = userDao;
        this.scholarshipDao = scholarshipDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean running = true;
        while (running) {
            System.out.println("\n========== ADMIN DASHBOARD ==========");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending Users");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Scholarship & Application Management");
            System.out.println("9. Logout");
            System.out.print("Choose (1-9): ");
            switch (scanner.nextLine().trim()) {
                case "1" -> viewUsers(userDao.getAllUsers(), "ALL USERS");
                case "2" -> viewUsers(userDao.getPendingUsers(), "PENDING USERS");
                case "3" -> approveUser(admin);
                case "4" -> addUser(admin);
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
                case "7" -> auditMenu();
                case "8" -> new AdminAcademicMenu(scanner, scholarshipDao,
                        applicationDao, activityLogDao).display(admin);
                case "9" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
        System.out.println("Admin logged out.");
    }

    private void viewUsers(List<User> users, String title) {
        System.out.println("\n========== " + title + " ==========");
        System.out.printf("%-6s %-24s %-30s %-10s %-10s%n",
                "ID", "NAME", "EMAIL", "ROLE", "STATUS");
        for (User u : users) {
            System.out.printf("%-6d %-24s %-30s %-10s %-10s%n",
                    u.userId(), u.fullName(), u.email(), u.role(), u.status());
        }
        if (users.isEmpty()) System.out.println("No users found.");
    }

    private void approveUser(User admin) {
        viewUsers(userDao.getPendingUsers(), "PENDING USERS");
        int id = readId("User ID to approve: ");
        if (id < 1) return;
        User selected = userDao.getUserById(id);
        if (selected == null || !"PENDING".equalsIgnoreCase(selected.status())) {
            System.out.println("User not found or account is not pending."); return;
        }
        if (!confirm("Approve " + selected.fullName() + "? (Y/N): ")) return;
        if (userDao.updateUserStatus(selected.email(), "APPROVED")) {
            log(admin, "USER_APPROVED", "Approved user ID " + id + " (" + selected.fullName() + ")");
            System.out.println("Account approved.");
        } else System.out.println("Approval failed.");
    }

    private void addUser(User admin) {
        System.out.print("Full name: "); String name = scanner.nextLine().trim();
        System.out.print("Email: "); String email = scanner.nextLine().trim().toLowerCase();
        System.out.print("Password (minimum 8 characters): "); String password = scanner.nextLine();
        System.out.print("Role (STUDENT/ADMIN): "); String role = scanner.nextLine().trim().toUpperCase();
        System.out.print("Department (optional): "); String dept = scanner.nextLine().trim();
        System.out.print("Status (PENDING/APPROVED): "); String status = scanner.nextLine().trim().toUpperCase();
        if (name.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
                || password.length() < 8 || !(role.equals("STUDENT") || role.equals("ADMIN"))
                || !(status.equals("PENDING") || status.equals("APPROVED"))) {
            System.out.println("Invalid user details."); return;
        }
        if (userDao.getUserByEmail(email) != null) { System.out.println("Email already exists."); return; }
        if (userDao.insertUser(new User(name, email, com.scc.util.PasswordUtil.hash(password),
                role, dept.isBlank() ? null : dept, status))) {
            log(admin, "USER_ADDED", "Added user " + name + " (" + email + ")");
            System.out.println("User added.");
        } else System.out.println("Could not add user.");
    }

    private void updateUser(User admin) {
        viewUsers(userDao.getAllUsers(), "ALL USERS");
        int id = readId("User ID to update: ");
        if (id < 1) return;
        User old = userDao.getUserById(id);
        if (old == null) { System.out.println("User not found."); return; }
        System.out.print("New role (STUDENT/ADMIN): "); String role = scanner.nextLine().trim().toUpperCase();
        System.out.print("New status (PENDING/APPROVED): "); String status = scanner.nextLine().trim().toUpperCase();
        if (!(role.equals("STUDENT") || role.equals("ADMIN"))
                || !(status.equals("PENDING") || status.equals("APPROVED"))) {
            System.out.println("Invalid role or status."); return;
        }
        if (!confirm("Confirm update? (Y/N): ")) return;
        if (userDao.updateUserRoleAndStatus(id, role, status)) {
            log(admin, "USER_UPDATED", "Updated user ID " + id + " role/status to " + role + "/" + status);
            System.out.println("User updated.");
        } else System.out.println("Update failed.");
    }

    private void deleteUser(User admin) {
        viewUsers(userDao.getAllUsers(), "ALL USERS");
        int id = readId("User ID to delete: ");
        if (id < 1) return;
        if (id == admin.userId()) { System.out.println("You cannot delete your own account."); return; }
        User selected = userDao.getUserById(id);
        if (selected == null) { System.out.println("User not found."); return; }
        if (!confirm("Permanently delete " + selected.fullName() + "? (Y/N): ")) return;
        if (userDao.deleteUserById(id)) {
            log(admin, "USER_DELETED", "Deleted user ID " + id + " (" + selected.fullName() + ")");
            System.out.println("User deleted.");
        } else System.out.println("Delete failed. Related records may prevent deletion.");
    }

    private void auditMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n========== SYSTEM AUDIT LOGS ==========");
            System.out.println("1. View All Logs\n2. Filter by User ID\n3. Clear Historical Logs\n4. Back");
            System.out.print("Choose (1-4): ");
            switch (scanner.nextLine().trim()) {
                case "1" -> printLogs(activityLogDao.getAllLogs());
                case "2" -> {
                    int id = readId("User ID: ");
                    if (id > 0) printLogs(activityLogDao.getLogsByUserId(id));
                }
                case "3" -> {
                    if (confirm("Type YES to clear all logs: ", "YES")) {
                        System.out.println(activityLogDao.clearAllLogs()
                                ? "Logs cleared." : "Could not clear logs.");
                    }
                }
                case "4" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void printLogs(List<ActivityLog> logs) {
        System.out.printf("%-5s %-8s %-22s %-50s %-22s%n",
                "ID", "USER", "ACTION", "DESCRIPTION", "TIMESTAMP");
        for (ActivityLog l : logs) {
            System.out.printf("%-5d %-8d %-22s %-50s %-22s%n",
                    l.logId(), l.userId(), l.actionType(), l.description(), l.timestamp());
        }
        if (logs.isEmpty()) System.out.println("No logs found.");
    }

    private void log(User admin, String action, String description) {
        if (!activityLogDao.logAction(admin.userId(), action, description))
            System.out.println("Warning: action succeeded but audit log failed.");
    }

    private int readId(String prompt) {
        System.out.print(prompt);
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid positive ID."); return -1;
        }
    }

    private boolean confirm(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim().equalsIgnoreCase("Y");
    }

    private boolean confirm(String prompt, String expected) {
        System.out.print(prompt);
        return scanner.nextLine().trim().equals(expected);
    }
}
