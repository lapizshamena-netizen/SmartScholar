package com.scc.model;

public record Scholarship(
        int scholarshipId,
        String title,
        String description,
        double grantAmount,
        double minGpa,
        int availableSlots,
        String deadline,
        int createdBy
) {}