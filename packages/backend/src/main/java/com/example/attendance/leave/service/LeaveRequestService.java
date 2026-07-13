package com.example.attendance.leave.service;

import com.example.attendance.leave.dto.LeaveRequestCreateRequest;
import com.example.attendance.leave.dto.LeaveRequestResponse;
import com.example.attendance.leave.dto.PendingLeaveRequestResponse;
import com.example.attendance.leave.entity.LeaveStatus;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestService {

    LeaveRequestResponse create(UUID requesterId, LeaveRequestCreateRequest request);

    List<LeaveRequestResponse> findByRequester(UUID requesterId, LeaveStatus status);

    List<PendingLeaveRequestResponse> findPending(UUID managerId);

    LeaveRequestResponse approve(UUID leaveRequestId, UUID approverId, Long version);

    LeaveRequestResponse reject(UUID leaveRequestId, UUID approverId, String reason, Long version);
}
