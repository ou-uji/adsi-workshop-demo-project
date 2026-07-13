package com.example.attendance.attendance.dto;

import com.example.attendance.attendance.entity.AttendanceRecord;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceRecordResponse(
    UUID id,
    LocalDate workDate,
    Instant clockIn,
    Instant clockOut,
    String clockInMemo,
    String clockOutMemo,
    boolean corrected,
    Long version
) {
    public static AttendanceRecordResponse from(AttendanceRecord record) {
        return new AttendanceRecordResponse(
            record.getId(),
            record.getWorkDate(),
            record.getClockIn(),
            record.getClockOut(),
            record.getClockInMemo(),
            record.getClockOutMemo(),
            record.isCorrected(),
            record.getVersion()
        );
    }
}
