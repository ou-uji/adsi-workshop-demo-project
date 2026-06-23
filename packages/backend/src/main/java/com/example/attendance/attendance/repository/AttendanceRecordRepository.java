package com.example.attendance.attendance.repository;

import com.example.attendance.attendance.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    List<AttendanceRecord> findByEmployeeIdAndWorkDateBetween(UUID employeeId, LocalDate start, LocalDate end);

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDateAndClockOutIsNull(UUID employeeId, LocalDate workDate);

    List<AttendanceRecord> findByEmployeeIdInAndWorkDateBetween(List<UUID> employeeIds, LocalDate start, LocalDate end);
}
