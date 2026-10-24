
package com.scc.ui;

import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.model.Scholarship;
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
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("            ADMIN DASHBOARD");
            System.out.println("==========================================");
            System.out.println("1. Create New Scholarship");
            System.out.println("2. View All Scholarships");
            System.out.println("3. Manage Users / Approve Accounts");
            System.out.println("4. Logout");
            System.out.print("Choose an option (1-4): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createScholarship(admin);
                case "2" -> viewScholarships();
                case "3" -> manageUsers();
                case "4" -> {
                    loggedIn = false;
                    System.out.println("Logged out successfully.");
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void manageUsers() {
        boolean managing = true;

        while (managing) {
            System.out.println("\n========== MANAGE USERS ==========");
            System.out.println("1. View All Users");
            System.out.println("2. Approve Pending User");
            System.out.println("3. Back to Admin Dashboard");
            System.out.print("Choose (1-3): ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> approveUser();
                case "3" -> managing = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void viewAllUsers() {
        List<User> users = userDao.getAllUsers();

        System.out.println("\n================ REGISTERED USERS ================");
        System.out.printf(
                "%-6s %-22s %-28s %-12s %-12s%n",
                "ID", "NAME", "EMAIL", "ROLE", "STATUS"
        );
        System.out.println(
                "--------------------------------------------------------------------------"
        );

        if (users.isEmpty()) {
            System.out.println("No registered users found.");
            return;
        }

        for (User user : users) {
            System.out.printf(
                    "%-6d %-22s %-28s %-12s %-12s%n",
                    user.userId(),
                    user.fullName(),
                    user.email(),
                    user.role(),
                    user.status()
            );
        }
    }

    private void approveUser() {
        viewAllUsers();

        System.out.print("\nEnter the USER ID to approve: ");
        String input = scanner.nextLine().trim();

        int userId;

        try {
            userId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid user ID.");
            return;
        }

        User selected = null;

        for (User user : userDao.getAllUsers()) {
            if (user.userId() == userId) {
                selected = user;
                break;
            }
        }

        if (selected == null) {
            System.out.println("User ID not found.");
            return;
        }

        if ("APPROVED".equalsIgnoreCase(selected.status())) {
            System.out.println("This account is already approved.");
            return;
        }

        if (!"PENDING".equalsIgnoreCase(selected.status())) {
            System.out.println("This account cannot be approved.");
            return;
        }

        System.out.println("\nSelected user:");
        System.out.println("ID: " + selected.userId());
        System.out.println("Name: " + selected.fullName());
        System.out.println("Email: " + selected.email());
        System.out.println("Role: " + selected.role());
        System.out.println("Current Status: " + selected.status());

        System.out.print("Approve this account? (Y/N): ");
        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("Approval cancelled.");
            return;
        }

        if (userDao.updateUserStatus(
                selected.email(), "APPROVED")) {
            System.out.println(
                    selected.fullName() +
                            " has been APPROVED successfully."
            );
        }
    }

    private void createScholarship(User admin) {
        System.out.println("\n--- CREATE NEW SCHOLARSHIP ---");

        try {
            System.out.print("Title: ");
            String title = scanner.nextLine().trim();

            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            System.out.print("Grant Amount: ");
            double grantAmount =
                    Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Minimum GPA Required: ");
            double minGpa =
                    Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Available Slots: ");
            int slots =
                    Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Deadline (YYYY-MM-DD): ");
            String deadline = scanner.nextLine().trim();

            if (title.isBlank() || deadline.isBlank()
                    || grantAmount < 0 || minGpa < 0 || slots < 0) {
                System.out.println("Please enter valid scholarship details.");
                return;
            }

            Scholarship scholarship = new Scholarship(
                    0,
                    title,
                    description,
                    grantAmount,
                    minGpa,
                    slots,
                    deadline,
                    admin.userId()
            );

            if (scholarshipDao.insertScholarship(scholarship)) {
                System.out.println("Scholarship created successfully!");
            } else {
                System.out.println("Failed to create scholarship.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Please try again.");
        }
    }

    private void viewScholarships() {
        System.out.println("\n--- AVAILABLE SCHOLARSHIPS ---");

        List<Scholarship> scholarships =
                scholarshipDao.getAllScholarships();

        if (scholarships.isEmpty()) {
            System.out.println("No scholarships posted yet.");
            return;
        }

        for (Scholarship s : scholarships) {
            System.out.printf(
                    "[%d] %s | Grant: %.2f | Min GPA: %.2f"
                            + " | Slots: %d | Deadline: %s%n",
                    s.scholarshipId(),
                    s.title(),
                    s.grantAmount(),
                    s.minGpa(),
                    s.availableSlots(),
                    s.deadline()
            );

            System.out.println("    " + s.description());
        }
    }
}
