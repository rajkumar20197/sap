package com.sap.sflit.service;

import com.sap.sflit.dto.EmployeeDTO;
import com.sap.sflit.entity.Employee;
import com.sap.sflit.multitenancy.TenantContext;
import com.sap.sflit.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Sprint 4: Unit Testing with Mockito
 * 
 * INTERVIEW TALKING POINTS:
 * - Mocking the DB layer allows fast, isolated testing of business logic.
 * - ThreadLocal (TenantContext) state must be explicitly managed/cleared in tests.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnAllEmployeesForTenant() {
        // Arrange
        Employee emp1 = Employee.builder().id(1L).name("Alice").department("HR").tenantId(TENANT_ID).build();
        Employee emp2 = Employee.builder().id(2L).name("Bob").department("IT").tenantId(TENANT_ID).build();
        when(employeeRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(emp1, emp2));

        // Act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Alice");
        verify(employeeRepository, times(1)).findByTenantId(TENANT_ID);
    }

    @Test
    void shouldFilterEmployeesByDepartment() {
        // Arrange
        Employee emp1 = Employee.builder().id(1L).name("Alice").department("HR").tenantId(TENANT_ID).build();
        Employee emp2 = Employee.builder().id(2L).name("Bob").department("IT").tenantId(TENANT_ID).build();
        when(employeeRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(emp1, emp2));

        // Act - filter by HR
        List<EmployeeDTO> result = employeeService.filterEmployees("HR", null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).department()).isEqualTo("HR");
    }

    @Test
    void shouldCreateEmployee() {
        // Arrange
        EmployeeDTO inputDto = EmployeeDTO.of("Charlie", "Finance", "charlie@test.com", "dummy-tenant");
        Employee savedEntity = Employee.builder()
                .id(3L).name("Charlie").department("Finance").email("charlie@test.com").tenantId(TENANT_ID).build();

        when(employeeRepository.existsByEmailAndTenantId("charlie@test.com", TENANT_ID)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEntity);

        // Act
        EmployeeDTO result = employeeService.createEmployee(inputDto);

        // Assert
        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.tenantId()).isEqualTo(TENANT_ID); // Should be overridden by TenantContext
        verify(employeeRepository).save(argThat(emp -> emp.getTenantId().equals(TENANT_ID)));
    }
}
