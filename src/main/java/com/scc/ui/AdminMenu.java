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

    public AdminMenu(UserDao userDao, ScholarshipDao scholarshipDao, Scanner scanner) {
        this.userDao = userDao;
        this.scholarshipDao = scholarshipDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("            ADMIN DASHBOARD               ");
            System.out.println("==========================================");
            System.out.println("1. Create New Scholarship");
            System.out.println("2. View All Scholarships");
            System.out.println("3. View All Registered Users");
            System.out.println("4. Logout");
            System.out.print("Choose an option (1-4): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createScholarship(admin);
                case "2" -> viewScholarships();
                case "3" -> viewAllUsers();
                case "4" -> {
                    loggedIn = false;
                    System.out.println("Logged out successfully.");
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void createScholarship(User admin) {
        System.out.println("\n--- CREATE NEW SCHOLARSHIP ---");
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String description = scanner.nextLine();
        System.out.print("Grant Amount ($): ");
        double grantAmount = Double.parseDouble(scanner.nextLine());
        System.out.print("Minimum GPA Required: ");
        double minGpa = Double.parseDouble(scanner.nextLine());
        System.out.print("Available Slots: ");
        int slots = Integer.parseInt(scanner.nextLine());
        System.out.print("Deadline (YYYY-MM-DD): ");
        String deadline = scanner.nextLine();

        Scholarship scholarship = new Scholarship(0, title, description, grantAmount, minGpa, slots, deadline, admin.userId());
        if (scholarshipDao.insertScholarship(scholarship)) {
            System.out.println("Scholarship created successfully!");
        } else {
            System.out.println("Failed to create scholarship.");
        }
    }

    private void viewScholarships() {
        System.out.println("\n--- AVAILABLE SCHOLARSHIPS ---");
        List<Scholarship> scholarships = scholarshipDao.getAllScholarships();
        if (scholarships.isEmpty()) {
            System.out.println("No scholarships posted yet.");
            return;
        }

        for (Scholarship s : scholarships) {
            System.out.printf("[%d] %s | Grant: $%.2f | Min GPA: %.2f | Slots: %d | Deadline: %s\n",
                    s.scholarshipId(), s.title(), s.grantAmount(), s.minGpa(), s.availableSlots(), s.deadline());
            System.out.println("    " + s.description());
        }
    }

    private void viewAllUsers() {
        System.out.println("\n--- USER LIST ---");
        List<User> users = userDao.getAllUsers();
        for (User u : users) {
            System.out.printf("ID: %d | Name: %s | Email: %s | Role: %s\n",
                    u.userId(), u.fullName(), u.email(), u.role());
        }
    }
}