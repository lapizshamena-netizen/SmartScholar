
package com.scc.model;

public record User(
        int userId,
        String fullName,
        String email,
        String passwordHash,
        String role,
        String department,
        String status
) {
    // Constructor with status
    public User(
            String fullName,
            String email,
            String passwordHash,
            String role,
            String department,
            String status
    ) {
        this(
                0,
                fullName,
                email,
                passwordHash,
                role,
                department,
                status
        );
    }

    // Backward-compatible constructor
    public User(
            int userId,
            String fullName,
            String email,
            String passwordHash,
            String role,
            String department
    ) {
        this(
                userId,
                fullName,
                email,
                passwordHash,
                role,
                department,
                "PENDING"
        );
    }

    // New accounts default to PENDING
    public User(
            String fullName,
            String email,
            String passwordHash,
            String role,
            String department
    ) {
        this(
                0,
                fullName,
                email,
                passwordHash,
                role,
                department,
                "PENDING"
        );
    }
}
