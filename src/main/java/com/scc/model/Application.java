package com.scc.model;

public record Application(
        int applicationId,
        int scholarshipId,
        String scholarshipTitle,
        int studentId,
        String studentName,
        int evaluatorId,
        String status,
        double gpaSubmitted,
        String submissionDate
) {}
