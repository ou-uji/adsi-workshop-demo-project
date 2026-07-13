package com.example.attendance.leave.service;

import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.leave.dto.LeaveRequestCreateRequest;
import com.example.attendance.leave.dto.LeaveRequestResponse;
import com.example.attendance.leave.dto.PendingLeaveRequestResponse;
import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.repository.LeaveRequestRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveRequestServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public LeaveRequestResponse create(UUID requesterId, LeaveRequestCreateRequest request) {
        var requester = findEmployeeOrThrow(requesterId);

        var leaveRequest = LeaveRequest.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .requester(requester)
                .targetDate(request.targetDate())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("LeaveRequest created: id={}, requester={}", saved.getId(), requesterId);
        return LeaveRequestResponse.from(saved);
    }

    @Override
    public List<LeaveRequestResponse> findByRequester(UUID requesterId, LeaveStatus status) {
        List<LeaveRequest> leaveRequests;
        if (status != null) {
            leaveRequests = leaveRequestRepository
                    .findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId, status);
        } else {
            leaveRequests = leaveRequestRepository
                    .findByRequesterIdOrderByCreatedAtDesc(requesterId);
        }
        return leaveRequests.stream()
                .map(LeaveRequestResponse::from)
                .toList();
    }

    @Override
    public List<PendingLeaveRequestResponse> findPending(UUID managerId) {
        var manager = findEmployeeOrThrow(managerId);
        var departmentId = manager.getDepartment().getId();
        var leaveRequests = leaveRequestRepository
                .findByRequesterDepartmentIdAndStatusOrderByCreatedAtDesc(
                        departmentId, LeaveStatus.PENDING);
        return leaveRequests.stream()
                .map(PendingLeaveRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(UUID leaveRequestId, UUID approverId, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveRequestId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leaveRequest, approver);
        validateVersion(leaveRequest, version);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprover(approver);

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("LeaveRequest approved: id={}, approver={}", leaveRequestId, approverId);
        return LeaveRequestResponse.from(saved);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(
            UUID leaveRequestId, UUID approverId, String reason, Long version) {
        var leaveRequest = findLeaveRequestOrThrow(leaveRequestId);
        var approver = findEmployeeOrThrow(approverId);

        validateApprover(leaveRequest, approver);
        validateVersion(leaveRequest, version);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprover(approver);
        leaveRequest.setRejectReason(reason);

        var saved = leaveRequestRepository.save(leaveRequest);
        log.info("LeaveRequest rejected: id={}, approver={}", leaveRequestId, approverId);
        return LeaveRequestResponse.from(saved);
    }

    private void validateApprover(LeaveRequest leaveRequest, Employee approver) {
        var requesterDeptId = leaveRequest.getRequester().getDepartment().getId();
        var approverDeptId = approver.getDepartment().getId();

        if (!approverDeptId.equals(requesterDeptId) || !approver.isManager()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the department manager can approve/reject leave requests");
        }
    }

    private void validateVersion(LeaveRequest leaveRequest, Long version) {
        if (!leaveRequest.getVersion().equals(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The leave request was modified by another user. Please refresh and try again.");
        }
    }

    private LeaveRequest findLeaveRequestOrThrow(UUID leaveRequestId) {
        return leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "LeaveRequest with id '%s' was not found"
                                .formatted(leaveRequestId)));
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Employee with id '%s' was not found".formatted(employeeId)));
    }
}
