package com.scc.model;

public record User(
        int userId,
        String fullName,
        String email,
        String passwordHash,
        String role,
        String department
) {
    public User(String fullName, String email, String passwordHash, String role, String department) {
        this(0, fullName, email, passwordHash, role, department);
    }
}