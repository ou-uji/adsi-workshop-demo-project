package com.example.attendance.employee.service;

import com.example.attendance.department.entity.Department;
import com.example.attendance.department.repository.DepartmentRepository;
import com.example.attendance.employee.dto.EmployeeCreateRequest;
import com.example.attendance.employee.dto.EmployeeUpdateRequest;
import com.example.attendance.employee.dto.ManagerRequest;
import com.example.attendance.employee.dto.RetireRequest;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeServiceImpl service;

    private Department department;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new EmployeeServiceImpl(employeeRepository, departmentRepository, passwordEncoder);

        department = Department.builder()
            .id(UUID.randomUUID())
            .name("Engineering")
            .build();

        employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
    }

    @Test
    @DisplayName("findAll: フィルタ条件に応じた結果を返す")
    @SuppressWarnings("unchecked")
    void findAll_withFilters_returnsFilteredResults() {
        // Arrange
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(employee), pageable, 1);
        when(employeeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // Act
        var result = service.findAll(pageable, department.getId(), Role.EMPLOYEE, false);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("田中太郎");
    }

    @Test
    @DisplayName("findById: 存在するIDで社員情報が返される")
    void findById_existingId_returnsEmployee() {
        // Arrange
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        // Act
        var result = service.findById(employee.getId());

        // Assert
        assertThat(result.name()).isEqualTo("田中太郎");
        assertThat(result.email()).isEqualTo("tanaka@example.com");
    }

    @Test
    @DisplayName("findById: 存在しないIDで例外が発生する")
    void findById_nonExistingId_throwsEntityNotFound() {
        // Arrange
        var unknownId = UUID.randomUUID();
        when(employeeRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.findById(unknownId))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("create: パスワードがBCryptハッシュ化されて保存される")
    void create_validRequest_passwordIsHashed() {
        // Arrange
        var request = new EmployeeCreateRequest(
            "田中太郎", "tanaka@example.com", "rawpassword",
            department.getId(), Role.EMPLOYEE, LocalDate.of(2024, 4, 1));
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("rawpassword")).thenReturn("$2a$encoded");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.create(request);

        // Assert
        var captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encoded");
    }

    @Test
    @DisplayName("create: メールアドレス重複で409例外が発生する")
    void create_duplicateEmail_throwsConflict() {
        // Arrange
        var request = new EmployeeCreateRequest(
            "田中太郎", "duplicate@example.com", "rawpassword",
            department.getId(), Role.EMPLOYEE, LocalDate.of(2024, 4, 1));
        when(employeeRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("create: 存在しない部署IDで404例外が発生する")
    void create_nonExistingDepartment_throwsNotFound() {
        // Arrange
        var unknownDeptId = UUID.randomUUID();
        var request = new EmployeeCreateRequest(
            "田中太郎", "tanaka@example.com", "rawpassword",
            unknownDeptId, Role.EMPLOYEE, LocalDate.of(2024, 4, 1));
        when(employeeRepository.existsByEmail("tanaka@example.com")).thenReturn(false);
        when(departmentRepository.findById(unknownDeptId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("update: 名前・メール・部署・ロールが更新される")
    void update_validRequest_updatesFields() {
        // Arrange
        var newDepartment = Department.builder()
            .id(UUID.randomUUID())
            .name("Sales")
            .build();
        var request = new EmployeeUpdateRequest(
            "佐藤花子", "sato@example.com",
            newDepartment.getId(), Role.ADMIN, LocalDate.of(2023, 4, 1));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("sato@example.com", employee.getId())).thenReturn(false);
        when(departmentRepository.findById(newDepartment.getId())).thenReturn(Optional.of(newDepartment));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = service.update(employee.getId(), request);

        // Assert
        assertThat(result.name()).isEqualTo("佐藤花子");
        assertThat(result.email()).isEqualTo("sato@example.com");
        assertThat(result.departmentId()).isEqualTo(newDepartment.getId());
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("retire: retireDateが設定される")
    void retire_activeEmployee_setsRetireDate() {
        // Arrange
        var request = new RetireRequest(LocalDate.of(2025, 3, 31));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = service.retire(employee.getId(), request);

        // Assert
        assertThat(result.retireDate()).isEqualTo(LocalDate.of(2025, 3, 31));
    }

    @Test
    @DisplayName("retire: 既に退職済みの社員で409例外が発生する")
    void retire_alreadyRetired_throwsConflict() {
        // Arrange
        employee.setRetireDate(LocalDate.of(2024, 12, 31));
        var request = new RetireRequest(LocalDate.of(2025, 3, 31));
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        // Act & Assert
        assertThatThrownBy(() -> service.retire(employee.getId(), request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("already retired");
    }

    @Test
    @DisplayName("setManager: isManagerがtrueになる")
    void setManager_noExistingManager_setsManagerTrue() {
        // Arrange
        var request = new ManagerRequest(true);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndIsManagerTrueAndIdNot(
            department.getId(), employee.getId())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = service.setManager(employee.getId(), request);

        // Assert
        assertThat(result.isManager()).isTrue();
    }

    @Test
    @DisplayName("setManager: 同部署に既に上長がいる場合409例外が発生する")
    void setManager_existingManagerInDepartment_throwsConflict() {
        // Arrange
        var request = new ManagerRequest(true);
        when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByDepartmentIdAndIsManagerTrueAndIdNot(
            department.getId(), employee.getId())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.setManager(employee.getId(), request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("already has a manager");
    }
}
