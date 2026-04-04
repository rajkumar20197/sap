package com.sap.sflit.repository;

import com.sap.sflit.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Employee Repository — Spring Data JPA
 *
 * INTERVIEW TIPS:
 *
 * 1. @EntityGraph: Solves the N+1 problem by eager-loading skills in a JOIN.
 *    Without it: fetching 100 employees fires 101 queries (1 for list + 1 per employee for skills).
 *    With it: 1 JOIN query fetches employees + skills together.
 *
 * 2. JPQL (@Query): Object-oriented query language. Uses entity class names and field names,
 *    NOT table and column names. JPA translates to SQL.
 *
 * 3. MULTI-TENANCY: ALL queries include tenant_id to ensure data isolation.
 *    No query should EVER run without a tenant_id filter in a multi-tenant system.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find all employees for a tenant.
     * @EntityGraph solves N+1 by JOIN FETCH-ing skills in one query.
     */
    @EntityGraph(attributePaths = {"skills"})
    List<Employee> findByTenantId(String tenantId);

    /**
     * Find by tenant + department — uses composite index (tenant_id, department).
     * EXPLAIN ANALYZE will show "Index Scan" here.
     */
    @EntityGraph(attributePaths = {"skills"})
    List<Employee> findByTenantIdAndDepartment(String tenantId, String department);

    /**
     * Find by tenant + ID (enforces tenant isolation on individual lookups).
     */
    @EntityGraph(attributePaths = {"skills"})
    Optional<Employee> findByIdAndTenantId(Long id, String tenantId);

    /**
     * Complex JPQL JOIN query — Sprint 2.
     * Demonstrates: JOIN with skills, WHERE clause, ORDER BY.
     * Run EXPLAIN ANALYZE on the generated SQL to verify index usage.
     */
    @Query("""
           SELECT DISTINCT e FROM Employee e
           JOIN e.skills s
           WHERE e.tenantId = :tenantId
             AND LOWER(s) IN :skills
           ORDER BY e.name
           """)
    @EntityGraph(attributePaths = {"skills"})
    List<Employee> findByTenantIdAndSkillsIn(
            @Param("tenantId") String tenantId,
            @Param("skills") List<String> skills
    );

    /**
     * Full-text style search within a tenant.
     * Demonstrates JPQL LIKE operator and LOWER() for case-insensitive search.
     */
    @Query("""
           SELECT e FROM Employee e
           WHERE e.tenantId = :tenantId
             AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(e.department) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')))
           """)
    List<Employee> searchByTenant(
            @Param("tenantId") String tenantId,
            @Param("query") String query
    );

    /**
     * Performance analytics — avg score by department for a tenant.
     * Demonstrates JPQL aggregate functions (AVG, GROUP BY).
     */
    @Query("""
           SELECT e.department, AVG(e.performanceScore)
           FROM Employee e
           WHERE e.tenantId = :tenantId
             AND e.performanceScore IS NOT NULL
           GROUP BY e.department
           ORDER BY AVG(e.performanceScore) DESC
           """)
    List<Object[]> getAvgPerformanceByDepartment(@Param("tenantId") String tenantId);

    boolean existsByEmailAndTenantId(String email, String tenantId);
}
