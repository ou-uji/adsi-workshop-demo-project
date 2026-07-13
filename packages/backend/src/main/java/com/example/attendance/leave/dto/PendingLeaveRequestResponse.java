package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PendingLeaveRequestResponse(
    UUID id,
    UUID requesterId,
    String requesterName,
    LocalDate targetDate,
    String reason,
    Long version,
    Instant createdAt
) {
    public static PendingLeaveRequestResponse from(LeaveRequest leaveRequest) {
        return new PendingLeaveRequestResponse(
            leaveRequest.getId(),
            leaveRequest.getRequester().getId(),
            leaveRequest.getRequester().getName(),
            leaveRequest.getTargetDate(),
            leaveRequest.getReason(),
            leaveRequest.getVersion(),
            leaveRequest.getCreatedAt()
        );
    }
}
