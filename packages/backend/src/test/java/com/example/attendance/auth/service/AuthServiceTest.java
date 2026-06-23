package com.example.attendance.auth.service;

import com.example.attendance.common.config.security.EmployeeUserDetails;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private EmployeeRepository employeeRepository;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        authService = new AuthServiceImpl(employeeRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ログイン中のユーザー情報が返される")
    void getCurrentUser_authenticatedUser_returnsAuthUserResponse() {
        // Arrange
        var employeeId = UUID.randomUUID();
        var departmentId = UUID.randomUUID();
        var department = Department.builder()
            .id(departmentId)
            .name("開発部")
            .build();
        var employee = Employee.builder()
            .id(employeeId)
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashed")
            .department(department)
            .role(Role.ADMIN)
            .isManager(true)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();

        var info = new EmployeeUserDetails.EmployeeInfo(
            employeeId, "田中太郎",
            departmentId, "開発部",
            Role.ADMIN, true
        );
        var userDetails = new EmployeeUserDetails(
            "tanaka@example.com",
            "hashed",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
            info
        );
        var authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        // Act
        var result = authService.getCurrentUser();

        // Assert
        assertThat(result.id()).isEqualTo(employeeId);
        assertThat(result.name()).isEqualTo("田中太郎");
        assertThat(result.email()).isEqualTo("tanaka@example.com");
        assertThat(result.departmentId()).isEqualTo(departmentId);
        assertThat(result.departmentName()).isEqualTo("開発部");
        assertThat(result.role()).isEqualTo(Role.ADMIN);
        assertThat(result.isManager()).isTrue();
    }
}
