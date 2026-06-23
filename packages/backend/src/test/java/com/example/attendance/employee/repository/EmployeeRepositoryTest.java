package com.example.attendance.employee.repository;

import com.example.attendance.common.config.JpaAuditingConfig;
import com.example.attendance.department.entity.Department;
import com.example.attendance.department.repository.DepartmentRepository;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager entityManager;

    private Department department1;
    private Department department2;

    @BeforeEach
    void setUp() {
        department1 = departmentRepository.save(Department.builder()
            .id(UUID.randomUUID())
            .name("Engineering")
            .build());

        department2 = departmentRepository.save(Department.builder()
            .id(UUID.randomUUID())
            .name("Sales")
            .build());

        entityManager.flush();
    }

    @Test
    @DisplayName("社員を保存して取得できる")
    void save_validEmployee_canBeRetrieved() {
        // Arrange
        var employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();

        // Act
        var saved = employeeRepository.save(employee);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var found = employeeRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("田中太郎");
        assertThat(found.get().getEmail()).isEqualTo("tanaka@example.com");
        assertThat(found.get().getDepartment().getId()).isEqualTo(department1.getId());
    }

    @Test
    @DisplayName("同一メールアドレスで保存するとDataIntegrityViolationExceptionが発生する")
    void save_duplicateEmail_throwsDataIntegrityViolation() {
        // Arrange
        var employee1 = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("duplicate@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        employeeRepository.save(employee1);
        entityManager.flush();

        var employee2 = Employee.builder()
            .id(UUID.randomUUID())
            .name("鈴木次郎")
            .email("duplicate@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();

        // Act & Assert
        assertThatThrownBy(() -> employeeRepository.saveAndFlush(employee2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("departmentIdで社員を絞り込める")
    void findAll_withDepartmentIdSpec_returnsFilteredResults() {
        // Arrange
        var emp1 = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        var emp2 = Employee.builder()
            .id(UUID.randomUUID())
            .name("鈴木次郎")
            .email("suzuki@example.com")
            .password("hashedpassword")
            .department(department2)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        employeeRepository.save(emp1);
        employeeRepository.save(emp2);
        entityManager.flush();

        // Act
        Specification<Employee> spec = EmployeeSpecifications.hasDepartmentId(department1.getId());
        var result = employeeRepository.findAll(spec);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("田中太郎");
    }

    @Test
    @DisplayName("existsByEmailが正しく動作する")
    void existsByEmail_existingEmail_returnsTrue() {
        // Arrange
        var employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        employeeRepository.save(employee);
        entityManager.flush();

        // Act & Assert
        assertThat(employeeRepository.existsByEmail("tanaka@example.com")).isTrue();
        assertThat(employeeRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("existsByDepartmentIdAndIsManagerTrueが正しく動作する")
    void existsByDepartmentIdAndIsManagerTrue_managerExists_returnsTrue() {
        // Arrange
        var manager = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(true)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        employeeRepository.save(manager);
        entityManager.flush();

        // Act & Assert
        assertThat(employeeRepository.existsByDepartmentIdAndIsManagerTrue(department1.getId())).isTrue();
        assertThat(employeeRepository.existsByDepartmentIdAndIsManagerTrue(department2.getId())).isFalse();
    }

    @Test
    @DisplayName("退職者を除外するSpecificationが正しく動作する")
    void findAll_withIsNotRetiredSpec_excludesRetiredEmployees() {
        // Arrange
        var active = Employee.builder()
            .id(UUID.randomUUID())
            .name("田中太郎")
            .email("tanaka@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        var retired = Employee.builder()
            .id(UUID.randomUUID())
            .name("鈴木次郎")
            .email("suzuki@example.com")
            .password("hashedpassword")
            .department(department1)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2020, 4, 1))
            .retireDate(LocalDate.of(2024, 3, 31))
            .build();
        employeeRepository.save(active);
        employeeRepository.save(retired);
        entityManager.flush();

        // Act
        var result = employeeRepository.findAll(EmployeeSpecifications.isNotRetired());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("田中太郎");
    }
}
