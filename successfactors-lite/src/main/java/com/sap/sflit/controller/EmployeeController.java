package com.sap.sflit.controller;

import com.sap.sflit.dto.EmployeeDTO;
import com.sap.sflit.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Employee REST Controller — Exposes Employee CRUD + OData-style filtering
 *
 * INTERVIEW TALKING POINTS:
 *
 * 1. REST DESIGN:
 *    - Plural nouns (/employees, not /employee)
 *    - HTTP verbs for actions (GET=read, POST=create, PUT=update, DELETE=delete)
 *    - Proper HTTP status codes (201 Created, 204 No Content, 404 Not Found)
 *    - API versioning via /api/v1/ prefix (supports future breaking changes without breaking clients)
 *
 * 2. ODATA-STYLE QUERY PARAMS:
 *    SAP uses OData which allows: GET /Entities?$filter=field eq 'value'&$select=field1,field2
 *    This controller demos OData-style filtering with Spring @RequestParam.
 *    Real OData would use Apache Olingo library — mention this in the interview.
 *
 * 3. REQUEST VALIDATION: @Valid triggers Bean Validation (JSR-380) on @RequestBody.
 *    Returns 400 automatically for constraint violations.
 *
 * 4. MULTI-TENANCY: The controller doesn't know which tenant it's serving —
 *    that's handled transparently by TenantInterceptor + TenantContext.
 *    This is the principle of Separation of Concerns.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * GET /api/v1/employees
     * OData-style: GET /api/v1/employees?department=HR&skill=Java&skill=Spring&q=alice
     *
     * Headers:
     *   X-Tenant-ID: company-a   (required for multi-tenancy)
     */
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getEmployees(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) List<String> skill,
            @RequestParam(required = false) String q  // OData-like $search
    ) {
        List<EmployeeDTO> employees;

        // If no filters, return all. Otherwise, apply stream-based filtering.
        if (department == null && (skill == null || skill.isEmpty()) && (q == null || q.isBlank())) {
            employees = employeeService.getAllEmployees();
        } else {
            employees = employeeService.filterEmployees(department, skill, q);
        }

        return ResponseEntity.ok(employees);
    }

    /**
     * GET /api/v1/employees/{id}
     * Returns 404 if not found OR if employee belongs to different tenant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/employees
     * Creates employee under current tenant. Returns 201 Created.
     */
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO dto) {
        EmployeeDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/v1/employees/{id}
     * Full update (all fields). Returns 404 if not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO dto
    ) {
        EmployeeDTO updated = employeeService.updateEmployee(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/employees/{id}
     * Returns 204 No Content on success. Returns 404 if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/employees/analytics/performance
     * Returns avg performance score by department.
     * Demonstrates JPQL aggregate queries (Sprint 2).
     */
    @GetMapping("/analytics/performance")
    public ResponseEntity<Map<String, Double>> getPerformanceAnalytics() {
        return ResponseEntity.ok(employeeService.getPerformanceByDepartment());
    }
}
