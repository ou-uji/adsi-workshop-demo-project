package com.example.attendance.leave.repository;

import com.example.attendance.common.config.JpaAuditingConfig;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository repository;

    @Autowired
    private EntityManager em;

    private Department department;
    private Department otherDepartment;
    private Employee employee;
    private Employee otherEmployee;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .build();
        em.persist(department);

        otherDepartment = Department.builder()
                .id(UUID.randomUUID())
                .name("Sales")
                .build();
        em.persist(otherDepartment);

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .password("hashed-password")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();
        em.persist(employee);

        otherEmployee = Employee.builder()
                .id(UUID.randomUUID())
                .name("山田花子")
                .email("yamada@example.com")
                .password("hashed-password")
                .department(otherDepartment)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();
        em.persist(otherEmployee);
        em.flush();
    }

    @Test
    @DisplayName("申請者IDで自分の申請のみ検索でき、他人の申請は含まれない")
    void findByRequesterId_returnsOwnRequestsOnly() {
        // Arrange: 自分2件 + 他人1件
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 20), LeaveStatus.PENDING));
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 21), LeaveStatus.APPROVED));
        em.persist(buildLeaveRequest(otherEmployee, LocalDate.of(2025, 10, 20), LeaveStatus.PENDING));
        em.flush();

        // Act
        var result = repository.findByRequesterIdOrderByCreatedAtDesc(employee.getId());

        // Assert: 自分の2件のみ（他人の申請は除外される）
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getRequester().getId().equals(employee.getId()));
    }

    @Test
    @DisplayName("申請者IDとステータスでPENDINGのみ検索できる")
    void findByRequesterIdAndStatus_filtersByStatus() {
        // Arrange
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 20), LeaveStatus.PENDING));
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 21), LeaveStatus.APPROVED));
        em.flush();

        // Act
        var result = repository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                employee.getId(), LeaveStatus.PENDING);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    @DisplayName("部署IDとステータスで自部署のPENDINGのみ検索でき、他部署・非PENDINGは除外される")
    void findByRequesterDepartmentIdAndStatus_returnsDeptPendingOnly() {
        // Arrange: 自部署PENDING1件 + 自部署APPROVED1件 + 他部署PENDING1件
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 20), LeaveStatus.PENDING));
        em.persist(buildLeaveRequest(employee, LocalDate.of(2025, 10, 21), LeaveStatus.APPROVED));
        em.persist(buildLeaveRequest(otherEmployee, LocalDate.of(2025, 10, 20), LeaveStatus.PENDING));
        em.flush();

        // Act
        var result = repository.findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                department.getId(), LeaveStatus.PENDING);

        // Assert: 自部署のPENDING1件のみ（他部署・APPROVEDは除外）
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(result.get(0).getRequester().getDepartment().getId())
                .isEqualTo(department.getId());
    }

    private LeaveRequest buildLeaveRequest(Employee requester, LocalDate targetDate, LeaveStatus status) {
        return LeaveRequest.builder()
                .id(UUID.randomUUID())
                .requester(requester)
                .targetDate(targetDate)
                .reason("私用のため")
                .status(status)
                .build();
    }
}
