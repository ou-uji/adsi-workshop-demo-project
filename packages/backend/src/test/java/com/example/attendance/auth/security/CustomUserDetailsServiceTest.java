package com.example.attendance.auth.security;

import com.example.attendance.common.config.security.CustomUserDetailsService;
import com.example.attendance.common.config.security.EmployeeUserDetails;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private EmployeeRepository employeeRepository;
    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        userDetailsService = new CustomUserDetailsService(employeeRepository);
    }

    @Test
    @DisplayName("有効な社員のメールアドレスで検索するとUserDetailsが返される")
    void loadUserByUsername_activeEmployee_returnsUserDetails() {
        // Arrange
        var departmentId = UUID.randomUUID();
        var department = Department.builder()
            .id(departmentId)
            .name("開発部")
            .build();
        var employeeId = UUID.randomUUID();
        var employee = Employee.builder()
            .id(employeeId)
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("$2a$10$hashedpassword")
            .department(department)
            .role(Role.ADMIN)
            .isManager(true)
            .hireDate(LocalDate.of(2024, 1, 1))
            .retireDate(null)
            .build();
        when(employeeRepository.findByEmail("tanaka@example.com"))
            .thenReturn(Optional.of(employee));

        // Act
        var userDetails = (EmployeeUserDetails) userDetailsService
            .loadUserByUsername("tanaka@example.com");

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo("tanaka@example.com");
        assertThat(userDetails.getEmployeeId()).isEqualTo(employeeId);
        assertThat(userDetails.getEmployeeName()).isEqualTo("田中太郎");
        assertThat(userDetails.getDepartmentId()).isEqualTo(departmentId);
        assertThat(userDetails.isManager()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("存在しないメールアドレスで検索するとUsernameNotFoundExceptionが発生する")
    void loadUserByUsername_nonExistentEmail_throwsException() {
        // Arrange
        when(employeeRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown@example.com"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("退職済み社員のメールアドレスで検索するとisEnabledがfalseになる")
    void loadUserByUsername_retiredEmployee_isDisabled() {
        // Arrange
        var department = Department.builder()
            .id(UUID.randomUUID())
            .name("開発部")
            .build();
        var employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("山田花子")
            .email("yamada@example.com")
            .password("$2a$10$hashedpassword")
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2020, 4, 1))
            .retireDate(LocalDate.of(2024, 3, 31))
            .build();
        when(employeeRepository.findByEmail("yamada@example.com"))
            .thenReturn(Optional.of(employee));

        // Act
        var userDetails = (EmployeeUserDetails) userDetailsService
            .loadUserByUsername("yamada@example.com");

        // Assert
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_EMPLOYEE");
    }
}
