package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.ApplicationDao;
import com.scc.dao.ScholarshipDao;
import com.scc.model.Application;
import com.scc.model.Scholarship;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class RoleMenu {
    private final Scanner scanner;
    private final String role;
    private final ScholarshipDao scholarshipDao;
    private final ApplicationDao applicationDao;
    private final ActivityLogDao activityLogDao;

    public RoleMenu(Scanner scanner, String role) {
        this(scanner, role, new ScholarshipDao(), new ApplicationDao(), new ActivityLogDao());
    }

    public RoleMenu(Scanner scanner, String role, ScholarshipDao scholarshipDao,
                    ApplicationDao applicationDao, ActivityLogDao activityLogDao) {
        this.scanner = scanner;
        this.role = role;
        this.scholarshipDao = scholarshipDao;
        this.applicationDao = applicationDao;
        this.activityLogDao = activityLogDao;
    }

    public void display(User user) {
        if (!"STUDENT".equalsIgnoreCase(role)) {
            System.out.println("Unsupported student menu role.");
            return;
        }
        boolean running = true;
        while (running) {
            System.out.println("\n========== STUDENT DASHBOARD ==========");
            System.out.println("Welcome, " + user.fullName());
            System.out.println("1. Browse Scholarships");
            System.out.println("2. Apply for Scholarship");
            System.out.println("3. View My Applications");
            System.out.println("4. Logout");
            System.out.print("Choose (1-4): ");
            switch (scanner.nextLine().trim()) {
                case "1" -> browseScholarships();
                case "2" -> apply(user);
                case "3" -> viewApplications(user);
                case "4" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void browseScholarships() {
        List<Scholarship> list = scholarshipDao.getAllScholarships();
        System.out.println("\n========== AVAILABLE SCHOLARSHIPS ==========");
        if (list.isEmpty()) {
            System.out.println("No scholarships are currently posted.");
            return;
        }
        for (Scholarship s : list) {
            System.out.printf("#%d | %s | Grant: %.2f | Min GPA: %.2f | Slots: %d | Deadline: %s%n",
                    s.scholarshipId(), s.title(), s.grantAmount(), s.minGpa(),
                    s.availableSlots(), s.deadline());
            System.out.println("  " + s.description());
        }
    }

    private void apply(User student) {
        browseScholarships();
        System.out.print("\nScholarship ID: ");
        int id = readPositiveInt();
        if (id < 1) return;
        System.out.print("Your GPA: ");
        double gpa;
        try {
            gpa = Double.parseDouble(scanner.nextLine().trim());
            if (gpa < 0 || gpa > 5) {
                System.out.println("Enter a GPA between 0 and 5.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid GPA.");
            return;
        }
        if (applicationDao.submit(student.userId(), id, gpa)) {
            boolean logged = activityLogDao.logAction(student.userId(), "APPLICATION_SUBMITTED",
                    "Submitted application for scholarship ID " + id);
            System.out.println("Application submitted successfully. Status: PENDING");
            if (!logged) System.out.println("Warning: audit log could not be saved.");
        } else {
            System.out.println("Application was not submitted. Check the scholarship ID, deadline, available slots, GPA requirement, and duplicate applications.");
        }
    }

    private void viewApplications(User student) {
        List<Application> list = applicationDao.getByStudent(student.userId());
        System.out.println("\n========== MY APPLICATIONS ==========");
        if (list.isEmpty()) {
            System.out.println("You have not submitted any applications.");
            return;
        }
        for (Application a : list) {
            System.out.printf("Application #%d | %s | GPA %.2f | %s | Submitted: %s%n",
                    a.applicationId(), a.scholarshipTitle(), a.gpaSubmitted(),
                    a.status(), a.submissionDate());
        }
    }

    private int readPositiveInt() {
        try {
            int value = Integer.parseInt(scanner.nextLine().trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid positive whole number.");
            return -1;
        }
    }
}
