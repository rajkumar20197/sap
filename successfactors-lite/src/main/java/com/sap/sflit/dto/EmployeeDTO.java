package com.sap.sflit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Java 17 Record DTO for Employee.
 *
 * WHY RECORDS? (Interview talking point)
 * - Immutable by default (no setters)
 * - Auto-generates: constructor, getters, equals(), hashCode(), toString()
 * - Reduces ~100 lines of boilerplate to ~5 lines
 * - Perfect for DTOs — data carriers that shouldn't be mutated
 *
 * JAVA 17 FEATURES USED:
 * - record keyword (Java 16+ stable)
 * - Compact constructor for validation (see below)
 */
public record EmployeeDTO(
        Long id,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Department is required")
        String department,

        @Email(message = "Must be a valid email")
        @NotBlank(message = "Email is required")
        String email,

        String title,

        @NotNull
        List<String> skills,

        Double performanceScore,

        String tenantId
) {
    /**
     * Compact constructor — runs before the auto-generated canonical constructor.
     * Used for defensive validation and normalization.
     */
    public EmployeeDTO {
        // Normalize skills to empty list if null (defensive)
        if (skills == null) {
            skills = List.of();
        }
        // Normalize department to title case
        if (department != null) {
            department = department.trim();
        }
    }

    /**
     * Factory method — named constructors are idiomatic with Records.
     */
    public static EmployeeDTO of(String name, String department, String email, String tenantId) {
        return new EmployeeDTO(null, name, department, email, null, List.of(), null, tenantId);
    }
}
