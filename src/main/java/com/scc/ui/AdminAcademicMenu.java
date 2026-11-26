package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.ApplicationDao;
import com.scc.dao.ScholarshipDao;
import com.scc.model.Application;
import com.scc.model.Scholarship;
import com.scc.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class AdminAcademicMenu {
    private final Scanner scanner;
    private final ScholarshipDao scholarshipDao;
    private final ApplicationDao applicationDao;
    private final ActivityLogDao activityLogDao;

    public AdminAcademicMenu(Scanner scanner, ScholarshipDao scholarshipDao,
                             ApplicationDao applicationDao, ActivityLogDao activityLogDao) {
        this.scanner = scanner;
        this.scholarshipDao = scholarshipDao;
        this.applicationDao = applicationDao;
        this.activityLogDao = activityLogDao;
    }

    public void display(User admin) {
        boolean running = true;
        while (running) {
            System.out.println("\n========== SCHOLARSHIP & APPLICATION MANAGEMENT ==========");
            System.out.println("1. Create Scholarship");
            System.out.println("2. View Scholarships");
            System.out.println("3. Update Scholarship");
            System.out.println("4. Delete Scholarship");
            System.out.println("5. View All Applications");
            System.out.println("6. Review Application");
            System.out.println("7. Back to Admin Dashboard");
            System.out.print("Choose (1-7): ");
            switch (scanner.nextLine().trim()) {
                case "1" -> create(admin);
                case "2" -> viewScholarships();
                case "3" -> update(admin);
                case "4" -> delete(admin);
                case "5" -> viewApplications();
                case "6" -> review(admin);
                case "7" -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void create(User admin) {
        System.out.println("\n========== CREATE SCHOLARSHIP ==========");
        System.out.print("Title: ");
        String title = scanner.nextLine().trim();
        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        Double grant = readDouble("Grant amount: ", 0, Double.MAX_VALUE);
        if (grant == null) return;
        Double gpa = readDouble("Minimum GPA (0-5): ", 0, 5);
        if (gpa == null) return;
        Integer slots = readInt("Available slots: ", 0);
        if (slots == null) return;
        String deadline = readDeadline();
        if (deadline == null || title.isBlank()) {
            System.out.println("Title and a valid deadline are required.");
            return;
        }
        Scholarship s = new Scholarship(0, title, description, grant, gpa, slots,
                deadline, admin.userId());
        if (scholarshipDao.insertScholarship(s)) {
            activityLogDao.logAction(admin.userId(), "SCHOLARSHIP_CREATED",
                    "Created scholarship: " + title);
            System.out.println("Scholarship created.");
        } else System.out.println("Could not create scholarship.");
    }

    private void viewScholarships() {
        List<Scholarship> list = scholarshipDao.getAllScholarships();
        System.out.println("\n========== SCHOLARSHIPS ==========");
        if (list.isEmpty()) { System.out.println("No scholarships found."); return; }
        for (Scholarship s : list) {
            System.out.printf("#%d | %s | Grant %.2f | GPA %.2f | Slots %d | Deadline %s%n  %s%n",
                    s.scholarshipId(), s.title(), s.grantAmount(), s.minGpa(),
                    s.availableSlots(), s.deadline(), s.description());
        }
    }

    private void update(User admin) {
        viewScholarships();
        Integer id = readInt("Scholarship ID to update: ", 1);
        if (id == null) return;
        Scholarship old = scholarshipDao.getById(id);
        if (old == null) { System.out.println("Scholarship not found."); return; }
        System.out.print("New title [" + old.title() + "]: ");
        String title = scanner.nextLine().trim();
        if (title.isBlank()) title = old.title();
        System.out.print("New description [" + old.description() + "]: ");
        String description = scanner.nextLine().trim();
        if (description.isBlank()) description = old.description();
        Double grant = readDouble("New grant amount [" + old.grantAmount() + "]: ", 0, Double.MAX_VALUE);
        if (grant == null) return;
        Double gpa = readDouble("New minimum GPA [" + old.minGpa() + "]: ", 0, 5);
        if (gpa == null) return;
        Integer slots = readInt("New available slots [" + old.availableSlots() + "]: ", 0);
        if (slots == null) return;
        String deadline = readDeadline();
        if (deadline == null) return;
        Scholarship changed = new Scholarship(id, title, description, grant, gpa, slots,
                deadline, old.createdBy());
        if (scholarshipDao.updateScholarship(changed)) {
            activityLogDao.logAction(admin.userId(), "SCHOLARSHIP_UPDATED",
                    "Updated scholarship ID " + id + ": " + title);
            System.out.println("Scholarship updated.");
        } else System.out.println("Update failed.");
    }

    private void delete(User admin) {
        viewScholarships();
        Integer id = readInt("Scholarship ID to delete: ", 1);
        if (id == null) return;
        Scholarship s = scholarshipDao.getById(id);
        if (s == null) { System.out.println("Scholarship not found."); return; }
        System.out.print("Type YES to delete " + s.title() + ": ");
        if (!"YES".equals(scanner.nextLine().trim())) {
            System.out.println("Delete cancelled."); return;
        }
        if (scholarshipDao.deleteScholarship(id)) {
            activityLogDao.logAction(admin.userId(), "SCHOLARSHIP_DELETED",
                    "Deleted scholarship ID " + id + ": " + s.title());
            System.out.println("Scholarship deleted.");
        } else System.out.println("Could not delete. Existing applications may reference this scholarship.");
    }

    private void viewApplications() {
        List<Application> list = applicationDao.getAll();
        System.out.println("\n========== SCHOLARSHIP APPLICATIONS ==========");
        if (list.isEmpty()) { System.out.println("No applications found."); return; }
        for (Application a : list) {
            System.out.printf("#%d | %s | Student: %s (#%d) | GPA %.2f | %s | Date %s%n",
                    a.applicationId(), a.scholarshipTitle(), a.studentName(),
                    a.studentId(), a.gpaSubmitted(), a.status(), a.submissionDate());
        }
    }

    private void review(User admin) {
        viewApplications();
        Integer id = readInt("Application ID to review: ", 1);
        if (id == null) return;
        System.out.print("Decision (A=Approve, R=Reject): ");
        String decision = scanner.nextLine().trim().toUpperCase();
        String status = switch (decision) {
            case "A" -> "APPROVED";
            case "R" -> "REJECTED";
            default -> null;
        };
        if (status == null) { System.out.println("Invalid decision."); return; }
        System.out.print("Confirm " + status + "? (Y/N): ");
        if (!"Y".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Review cancelled."); return;
        }
        if (applicationDao.review(id, admin.userId(), status)) {
            activityLogDao.logAction(admin.userId(), "APPLICATION_" + status,
                    "Reviewed application ID " + id + " as " + status);
            System.out.println("Application " + status + ".");
        } else System.out.println("Review failed. The application may not be pending or scholarship slots may be full.");
    }

    private Double readDouble(String prompt, double min, double max) {
        System.out.print(prompt);
        try {
            double v = Double.parseDouble(scanner.nextLine().trim());
            if (!Double.isFinite(v) || v < min || v > max) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number or value out of range.");
            return null;
        }
    }

    private Integer readInt(String prompt, int min) {
        System.out.print(prompt);
        try {
            int v = Integer.parseInt(scanner.nextLine().trim());
            if (v < min) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            System.out.println("Invalid whole number.");
            return null;
        }
    }

    private String readDeadline() {
        System.out.print("Deadline (YYYY-MM-DD): ");
        String value = scanner.nextLine().trim();
        try {
            LocalDate.parse(value);
            return value;
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date. Use YYYY-MM-DD.");
            return null;
        }
    }
}
