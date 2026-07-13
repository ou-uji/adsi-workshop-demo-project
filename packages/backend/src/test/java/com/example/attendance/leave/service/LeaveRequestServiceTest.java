package com.example.attendance.leave.service;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveRequestCreateRequest;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveRequestServiceImpl service;

    private Department department;
    private Employee employee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        service = new LeaveRequestServiceImpl(leaveRequestRepository, employeeRepository);

        department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .build();

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .password("hashed")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();

        manager = Employee.builder()
                .id(UUID.randomUUID())
                .name("佐藤次郎")
                .email("sato@example.com")
                .password("hashed")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(true)
                .hireDate(LocalDate.of(2020, 4, 1))
                .build();
    }

    @Nested
    @DisplayName("有給申請作成")
    class Create {

        @Test
        @DisplayName("有給申請するとPENDINGで保存される")
        void create_validRequest_createsPendingLeaveRequest() {
            // Arrange
            var request = new LeaveRequestCreateRequest(
                    LocalDate.of(2025, 10, 20),
                    "私用のため"
            );
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.create(employee.getId(), request);

            // Assert
            assertThat(result.status()).isEqualTo(LeaveStatus.PENDING);
            assertThat(result.reason()).isEqualTo("私用のため");
            assertThat(result.requesterId()).isEqualTo(employee.getId());

            var captor = ArgumentCaptor.forClass(LeaveRequest.class);
            verify(leaveRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getTargetDate()).isEqualTo(LocalDate.of(2025, 10, 20));
        }

        @Test
        @DisplayName("過去日でも申請できる（対象日に制約なし）")
        void create_pastDate_succeeds() {
            // Arrange
            var request = new LeaveRequestCreateRequest(
                    LocalDate.of(2020, 1, 1),
                    "過去分の申請"
            );
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.create(employee.getId(), request);

            // Assert
            assertThat(result.status()).isEqualTo(LeaveStatus.PENDING);
            assertThat(result.targetDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        }
    }

    @Nested
    @DisplayName("自分の申請一覧")
    class FindByRequester {

        @Test
        @DisplayName("ログインユーザーの申請のみ返す")
        void findByRequester_returnsOwnRequests() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findByRequesterIdOrderByCreatedAtDesc(employee.getId()))
                    .thenReturn(List.of(leaveRequest));

            // Act
            var results = service.findByRequester(employee.getId(), null);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).requesterId()).isEqualTo(employee.getId());
        }

        @Test
        @DisplayName("ステータスフィルタでPENDINGのみ返す")
        void findByRequester_withStatusFilter_returnsPendingOnly() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(
                    employee.getId(), LeaveStatus.PENDING))
                    .thenReturn(List.of(leaveRequest));

            // Act
            var results = service.findByRequester(employee.getId(), LeaveStatus.PENDING);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).status()).isEqualTo(LeaveStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("承認待ち一覧")
    class FindPending {

        @Test
        @DisplayName("自部署メンバーのPENDINGのみ返す")
        void findPending_returnsOwnDepartmentPending() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(employeeRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
            when(leaveRequestRepository.findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                    department.getId(), LeaveStatus.PENDING))
                    .thenReturn(List.of(leaveRequest));

            // Act
            var results = service.findPending(manager.getId());

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0).requesterName()).isEqualTo("田中太郎");
        }
    }

    @Nested
    @DisplayName("承認")
    class Approve {

        @Test
        @DisplayName("同一部署の上長が承認するとAPPROVEDになりapproverが記録される")
        void approve_byManager_setsApproved() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.approve(leaveRequest.getId(), manager.getId(), 0L);

            // Assert
            assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
            assertThat(result.approverId()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("上長の自己承認が正常に動作する")
        void approve_selfApproval_succeeds() {
            // Arrange
            var leaveRequest = buildPending(manager);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.approve(leaveRequest.getId(), manager.getId(), 0L);

            // Assert
            assertThat(result.status()).isEqualTo(LeaveStatus.APPROVED);
            assertThat(result.approverId()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("承認者が部署の上長でない場合は403エラー")
        void approve_notManager_throwsForbidden() {
            // Arrange
            var otherDepartment = Department.builder()
                    .id(UUID.randomUUID())
                    .name("Sales")
                    .build();
            var otherManager = Employee.builder()
                    .id(UUID.randomUUID())
                    .name("鈴木三郎")
                    .email("suzuki@example.com")
                    .password("hashed")
                    .department(otherDepartment)
                    .role(Role.EMPLOYEE)
                    .isManager(true)
                    .hireDate(LocalDate.of(2020, 4, 1))
                    .build();
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(otherManager.getId()))
                    .thenReturn(Optional.of(otherManager));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.approve(leaveRequest.getId(), otherManager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }

        @Test
        @DisplayName("承認しても勤怠レコードは作成/更新されない（leaveRequestのみ保存）")
        void approve_doesNotTouchAttendance() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            service.approve(leaveRequest.getId(), manager.getId(), 0L);

            // Assert: leaveRequest だけが保存され、他の副作用は無い
            verify(leaveRequestRepository).save(any(LeaveRequest.class));
            // employeeRepository は参照のみ（save されない）
            verify(employeeRepository, never()).save(any(Employee.class));
        }
    }

    @Nested
    @DisplayName("却下")
    class Reject {

        @Test
        @DisplayName("却下するとREJECTEDになり理由が保存される")
        void reject_setsStatusAndReason() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));
            when(leaveRequestRepository.save(any(LeaveRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.reject(
                    leaveRequest.getId(), manager.getId(), "業務都合により承認できません", 0L);

            // Assert
            assertThat(result.status()).isEqualTo(LeaveStatus.REJECTED);
            assertThat(result.rejectReason()).isEqualTo("業務都合により承認できません");
        }

        @Test
        @DisplayName("却下も他部署/非上長は403エラー")
        void reject_notManager_throwsForbidden() {
            // Arrange
            var leaveRequest = buildPending(employee);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(employee.getId()))
                    .thenReturn(Optional.of(employee)); // 非上長

            // Act & Assert
            assertThatThrownBy(() ->
                    service.reject(leaveRequest.getId(), employee.getId(), "却下理由", 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("403");
        }
    }

    @Nested
    @DisplayName("楽観ロック")
    class OptimisticLock {

        @Test
        @DisplayName("承認: バージョン不一致時はコンフリクトエラー")
        void approve_versionMismatch_throwsConflict() {
            // Arrange
            var leaveRequest = buildPending(employee);
            leaveRequest.setVersion(1L);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.approve(leaveRequest.getId(), manager.getId(), 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("409");
        }

        @Test
        @DisplayName("却下: バージョン不一致時はコンフリクトエラー")
        void reject_versionMismatch_throwsConflict() {
            // Arrange
            var leaveRequest = buildPending(employee);
            leaveRequest.setVersion(1L);
            when(leaveRequestRepository.findById(leaveRequest.getId()))
                    .thenReturn(Optional.of(leaveRequest));
            when(employeeRepository.findById(manager.getId()))
                    .thenReturn(Optional.of(manager));

            // Act & Assert
            assertThatThrownBy(() ->
                    service.reject(leaveRequest.getId(), manager.getId(), "却下理由", 0L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("409");
        }
    }

    private LeaveRequest buildPending(Employee requester) {
        return LeaveRequest.builder()
                .id(UUID.randomUUID())
                .requester(requester)
                .targetDate(LocalDate.of(2025, 10, 20))
                .reason("私用のため")
                .status(LeaveStatus.PENDING)
                .version(0L)
                .build();
    }
}
