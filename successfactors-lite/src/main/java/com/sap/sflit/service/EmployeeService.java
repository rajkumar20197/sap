package com.sap.sflit.service;

import com.sap.sflit.dto.EmployeeDTO;
import com.sap.sflit.entity.Employee;
import com.sap.sflit.multitenancy.TenantContext;
import com.sap.sflit.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Employee Service — Core Business Logic Layer
 *
 * INTERVIEW TALKING POINTS:
 *
 * 1. STREAM API: Used for in-memory filtering, transformation, and aggregation.
 *    Useful when full DB query is overkill, or for post-retrieval processing.
 *
 * 2. @Transactional: Ensures atomicity. If any operation fails, the entire transaction
 *    rolls back. READ operations use readOnly=true for performance (no dirty checking).
 *
 * 3. MAPPER PATTERN: toDTO() and toEntity() methods keep the service layer clean —
 *    controllers deal only with DTOs, never with JPA entities (separation of concerns).
 *
 * 4. ODATA-STYLE QUERY: The filterEmployees() method mimics OData's $filter behavior
 *    using Java Streams — demonstrating both concepts in one place.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /**
     * Get all employees for the current tenant.
     * Tenant is automatically read from TenantContext (set by TenantInterceptor).
     */
    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        String tenantId = TenantContext.getCurrentTenant();
        log.info("Fetching all employees for tenant: {}", tenantId);
        return employeeRepository.findByTenantId(tenantId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get employee by ID — enforces tenant isolation.
     * Throws if not found OR if found but belongs to different tenant.
     */
    @Transactional(readOnly = true)
    public Optional<EmployeeDTO> getEmployeeById(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        return employeeRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toDTO);
    }

    /**
     * STREAM API SHOWCASE:
     * Filter employees by department and/or skills using Java Streams.
     *
     * INTERVIEW NOTE: This demonstrates METHOD CHAINING and FUNCTIONAL PROGRAMMING in Java.
     * - filter(): intermediate operation, lazy
     * - map(): transforms each element
     * - collect(): terminal operation, triggers evaluation
     *
     * OData equivalent: GET /employees?$filter=department eq 'HR' and skills/any(s: s eq 'Java')
     */
    @Transactional(readOnly = true)
    public List<EmployeeDTO> filterEmployees(String department, List<String> skills, String searchQuery) {
        String tenantId = TenantContext.getCurrentTenant();
        List<Employee> all = employeeRepository.findByTenantId(tenantId);

        return all.stream()
                // Filter by department (case-insensitive) if provided
                .filter(e -> department == null || department.isBlank()
                        || e.getDepartment().equalsIgnoreCase(department))
                // Filter by skills — employee must have ALL requested skills
                .filter(e -> skills == null || skills.isEmpty()
                        || e.getSkills().stream()
                           .anyMatch(s -> skills.stream()
                                   .anyMatch(req -> s.equalsIgnoreCase(req))))
                // Filter by free-text search across name, title, department
                .filter(e -> searchQuery == null || searchQuery.isBlank()
                        || e.getName().toLowerCase().contains(searchQuery.toLowerCase())
                        || (e.getTitle() != null && e.getTitle().toLowerCase().contains(searchQuery.toLowerCase()))
                        || e.getDepartment().toLowerCase().contains(searchQuery.toLowerCase()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new employee under the current tenant.
     */
    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();

        if (employeeRepository.existsByEmailAndTenantId(dto.email(), tenantId)) {
            throw new IllegalArgumentException("Employee with email " + dto.email() + " already exists in tenant " + tenantId);
        }

        Employee entity = toEntity(dto);
        entity.setTenantId(tenantId); // Always set from context, never trust client
        Employee saved = employeeRepository.save(entity);
        log.info("Created employee {} for tenant {}", saved.getId(), tenantId);
        return toDTO(saved);
    }

    /**
     * Update an existing employee — validates tenant ownership first.
     */
    @Transactional
    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        String tenantId = TenantContext.getCurrentTenant();
        Employee existing = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Employee " + id + " not found for tenant " + tenantId));

        existing.setName(dto.name());
        existing.setDepartment(dto.department());
        existing.setEmail(dto.email());
        existing.setTitle(dto.title());
        existing.setSkills(dto.skills());
        existing.setPerformanceScore(dto.performanceScore());

        Employee saved = employeeRepository.save(existing);
        return toDTO(saved);
    }

    /**
     * Delete — validates tenant ownership before deletion.
     */
    @Transactional
    public void deleteEmployee(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        Employee existing = employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Employee " + id + " not found for tenant " + tenantId));
        employeeRepository.delete(existing);
        log.info("Deleted employee {} for tenant {}", id, tenantId);
    }

    /**
     * Performance analytics using JPQL aggregate query.
     * Returns Map of department -> avgScore.
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getPerformanceByDepartment() {
        String tenantId = TenantContext.getCurrentTenant();
        return employeeRepository.getAvgPerformanceByDepartment(tenantId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Double) row[1]
                ));
    }

    // ========== MAPPER METHODS ==========

    /**
     * Entity → DTO (for responses)
     * Records are immutable, so we use the canonical constructor.
     */
    public EmployeeDTO toDTO(Employee entity) {
        return new EmployeeDTO(
                entity.getId(),
                entity.getName(),
                entity.getDepartment(),
                entity.getEmail(),
                entity.getTitle(),
                entity.getSkills(),
                entity.getPerformanceScore(),
                entity.getTenantId()
        );
    }

    /**
     * DTO → Entity (for creation/updates)
     * Uses Lombok @Builder pattern for readable construction.
     */
    private Employee toEntity(EmployeeDTO dto) {
        return Employee.builder()
                .name(dto.name())
                .department(dto.department())
                .email(dto.email())
                .title(dto.title())
                .skills(dto.skills())
                .performanceScore(dto.performanceScore())
                .build();
    }
}
