package com.sap.sflit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee JPA Entity
 *
 * MULTI-TENANCY PATTERN (Interview talking point - Sprint 2):
 * Every table has a tenant_id column. This is the "shared database, shared schema"
 * multi-tenancy model — the most common approach at SAP SuccessFactors.
 * All queries are automatically scoped to the current tenant via TenantFilter + ThreadLocal.
 *
 * JPA CONCEPTS TO KNOW:
 * - @Entity: marks class as a JPA-managed entity
 * - @Table: maps to a specific DB table, allows index definitions
 * - @Column: configures column-level constraints
 * - @ElementCollection: maps a List<String> to a separate joined table
 * - @Index: creates DB indexes for query optimization (Sprint 2)
 */
@Entity
@Table(
    name = "employees",
    indexes = {
        // Composite index: tenant_id always queried with department
        // EXPLAIN ANALYZE will show "Index Scan" instead of "Seq Scan" after this
        @Index(name = "idx_employee_tenant_dept", columnList = "tenant_id, department"),
        @Index(name = "idx_employee_tenant", columnList = "tenant_id"),
        @Index(name = "idx_employee_email", columnList = "email", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MULTI-TENANCY: This column isolates data between companies/clients.
     * Sprint 2: TenantFilter ensures every request sets this via ThreadLocal.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false, unique = true)
    private String email;

    private String title;

    /**
     * Stores skills as a separate table: employee_skills(employee_id, skill)
     * This avoids storing arrays in a single column (violates 1NF).
     *
     * N+1 PROBLEM NOTE: Without @EntityGraph or JOIN FETCH, loading skills
     * for each employee in a list will fire N+1 queries. See EmployeeRepository.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column(name = "performance_score")
    private Double performanceScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
