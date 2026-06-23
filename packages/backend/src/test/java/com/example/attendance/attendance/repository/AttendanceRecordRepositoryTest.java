package com.example.attendance.attendance.repository;

import com.example.attendance.attendance.entity.AttendanceRecord;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AttendanceRecordRepositoryTest {

    @Autowired
    private AttendanceRecordRepository repository;

    @Autowired
    private EntityManager em;

    private Employee employee;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .build();
        em.persist(department);

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
        em.flush();
    }

    @Test
    @DisplayName("社員IDと勤務日で打刻レコードが検索できる")
    void findByEmployeeIdAndWorkDate_existingRecords_returnsRecords() {
        // Arrange
        var today = LocalDate.of(2025, 1, 15);
        var record = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .workDate(today)
                .clockIn(Instant.parse("2025-01-14T23:00:00Z"))
                .clockOut(Instant.parse("2025-01-15T08:00:00Z"))
                .corrected(false)
                .build();
        em.persist(record);
        em.flush();

        // Act
        var result = repository.findByEmployeeIdAndWorkDate(employee.getId(), today);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployee().getId()).isEqualTo(employee.getId());
        assertThat(result.get(0).getWorkDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("月の範囲でレコードが検索できる")
    void findByEmployeeIdAndWorkDateBetween_monthRange_returnsMatchingRecords() {
        // Arrange
        var jan15 = LocalDate.of(2025, 1, 15);
        var jan20 = LocalDate.of(2025, 1, 20);
        var feb01 = LocalDate.of(2025, 2, 1);

        em.persist(buildRecord(employee, jan15, "2025-01-14T23:00:00Z", "2025-01-15T08:00:00Z"));
        em.persist(buildRecord(employee, jan20, "2025-01-19T23:00:00Z", "2025-01-20T08:00:00Z"));
        em.persist(buildRecord(employee, feb01, "2025-01-31T23:00:00Z", "2025-02-01T08:00:00Z"));
        em.flush();

        // Act
        var result = repository.findByEmployeeIdAndWorkDateBetween(
                employee.getId(),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31)
        );

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getWorkDate().getMonthValue() == 1);
    }

    @Test
    @DisplayName("未退勤レコードが検索できる")
    void findByEmployeeIdAndWorkDateAndClockOutIsNull_openRecord_returnsRecord() {
        // Arrange
        var today = LocalDate.of(2025, 1, 15);
        var openRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .workDate(today)
                .clockIn(Instant.parse("2025-01-14T23:00:00Z"))
                .corrected(false)
                .build();
        em.persist(openRecord);
        em.flush();

        // Act
        var result = repository.findByEmployeeIdAndWorkDateAndClockOutIsNull(employee.getId(), today);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getClockOut()).isNull();
    }

    @Test
    @DisplayName("退勤済みの場合は未退勤レコード検索で空が返る")
    void findByEmployeeIdAndWorkDateAndClockOutIsNull_closedRecord_returnsEmpty() {
        // Arrange
        var today = LocalDate.of(2025, 1, 15);
        var closedRecord = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .workDate(today)
                .clockIn(Instant.parse("2025-01-14T23:00:00Z"))
                .clockOut(Instant.parse("2025-01-15T08:00:00Z"))
                .corrected(false)
                .build();
        em.persist(closedRecord);
        em.flush();

        // Act
        var result = repository.findByEmployeeIdAndWorkDateAndClockOutIsNull(employee.getId(), today);

        // Assert
        assertThat(result).isEmpty();
    }

    private AttendanceRecord buildRecord(Employee emp, LocalDate workDate, String clockIn, String clockOut) {
        return AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .employee(emp)
                .workDate(workDate)
                .clockIn(Instant.parse(clockIn))
                .clockOut(clockOut != null ? Instant.parse(clockOut) : null)
                .corrected(false)
                .build();
    }
}
