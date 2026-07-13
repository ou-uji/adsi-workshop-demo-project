package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.Size;

/**
 * 打刻（出勤・退勤）時の任意メモ。body 自体を省略でき、memo も null 可（空欄OK）。
 */
public record ClockRequest(
    @Size(max = 200) String memo
) {}
