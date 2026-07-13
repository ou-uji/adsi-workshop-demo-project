package com.example.attendance.leave.controller;

import com.example.attendance.leave.dto.LeaveRejectRequest;
import com.example.attendance.leave.dto.LeaveRequestCreateRequest;
import com.example.attendance.leave.dto.LeaveRequestResponse;
import com.example.attendance.leave.dto.PendingLeaveRequestResponse;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.service.LeaveRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestResponse create(
            @RequestParam UUID requesterId,
            @Valid @RequestBody LeaveRequestCreateRequest request) {
        return leaveRequestService.create(requesterId, request);
    }

    @GetMapping
    public List<LeaveRequestResponse> findByRequester(
            @RequestParam UUID requesterId,
            @RequestParam(required = false) LeaveStatus status) {
        return leaveRequestService.findByRequester(requesterId, status);
    }

    @GetMapping("/pending")
    public List<PendingLeaveRequestResponse> findPending(@RequestParam UUID managerId) {
        return leaveRequestService.findPending(managerId);
    }

    @PatchMapping("/{id}/approve")
    public LeaveRequestResponse approve(
            @PathVariable UUID id,
            @RequestParam UUID approverId,
            @RequestParam Long version) {
        return leaveRequestService.approve(id, approverId, version);
    }

    @PatchMapping("/{id}/reject")
    public LeaveRequestResponse reject(
            @PathVariable UUID id,
            @RequestParam UUID approverId,
            @Valid @RequestBody LeaveRejectRequest request) {
        return leaveRequestService.reject(id, approverId, request.reason(), request.version());
    }
}
