package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveRequest;
import com.example.attendance.leave.entity.LeaveStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
    UUID id,
    UUID requesterId,
    String requesterName,
    UUID approverId,
    String approverName,
    LocalDate targetDate,
    String reason,
    LeaveStatus status,
    String rejectReason,
    Long version,
    Instant createdAt
) {
    public static LeaveRequestResponse from(LeaveRequest leaveRequest) {
        return new LeaveRequestResponse(
            leaveRequest.getId(),
            leaveRequest.getRequester().getId(),
            leaveRequest.getRequester().getName(),
            leaveRequest.getApprover() != null
                ? leaveRequest.getApprover().getId() : null,
            leaveRequest.getApprover() != null
                ? leaveRequest.getApprover().getName() : null,
            leaveRequest.getTargetDate(),
            leaveRequest.getReason(),
            leaveRequest.getStatus(),
            leaveRequest.getRejectReason(),
            leaveRequest.getVersion(),
            leaveRequest.getCreatedAt()
        );
    }
}
