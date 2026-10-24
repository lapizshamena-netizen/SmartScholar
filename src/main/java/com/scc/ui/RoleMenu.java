package com.scc.ui;

import com.scc.model.User;

import java.util.Scanner;

public class RoleMenu {

    private final Scanner scanner;
    private final String role;

    public RoleMenu(Scanner scanner, String role) {
        this.scanner = scanner;
        this.role = role;
    }

    public void display(User user) {
        System.out.println(
                "\n========== " + role + " DASHBOARD =========="
        );

        System.out.println(
                "Welcome, " + user.fullName() + "."
        );

        System.out.println(
                "Role-specific features can be added here."
        );

        System.out.println(
                "Press Enter to return to the main menu."
        );

        scanner.nextLine();
    }
}