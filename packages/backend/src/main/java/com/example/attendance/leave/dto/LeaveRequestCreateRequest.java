package com.example.attendance.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestCreateRequest(
    @NotNull LocalDate targetDate,
    @NotNull @Size(min = 1, max = 500) String reason
) {}
