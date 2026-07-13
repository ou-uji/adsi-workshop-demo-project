package com.example.attendance.attendance.controller;

import com.example.attendance.attendance.domain.AttendanceStatus;
import com.example.attendance.attendance.dto.AttendanceHistoryResponse;
import com.example.attendance.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.attendance.dto.DailyAttendanceResponse;
import com.example.attendance.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.attendance.dto.TodayStatusResponse;
import com.example.attendance.attendance.service.AttendanceService;
import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AttendanceController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(AttendanceControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class AttendanceControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("POST /api/attendance/clock-in は201を返す（body なし・後方互換）")
    void clockIn_validRequest_returns201() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                null,
                null,
                false,
                0L
        );
        when(attendanceService.clockIn(EMPLOYEE_ID, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workDate").value("2025-01-15"))
                .andExpect(jsonPath("$.clockOut").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in はメモ付き body で201を返す")
    void clockIn_withMemoBody_returns201() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                "電車遅延のため遅刻",
                null,
                false,
                0L
        );
        when(attendanceService.clockIn(EMPLOYEE_ID, "電車遅延のため遅刻")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType("application/json")
                        .content("{\"memo\":\"電車遅延のため遅刻\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clockInMemo").value("電車遅延のため遅刻"));
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in は200文字ちょうどのメモで201を返す（境界値）")
    void clockIn_memoExactly200_returns201() throws Exception {
        // Arrange: 200 文字ちょうど
        var memo200 = "あ".repeat(200);
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                memo200,
                null,
                false,
                0L
        );
        when(attendanceService.clockIn(EMPLOYEE_ID, memo200)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType("application/json")
                        .content("{\"memo\":\"" + memo200 + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in は201文字メモで400を返す")
    void clockIn_memoOver200_returns400() throws Exception {
        // Arrange: 201 文字
        var longMemo = "あ".repeat(201);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType("application/json")
                        .content("{\"memo\":\"" + longMemo + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-out は200を返す")
    void clockOut_validRequest_returns200() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                null,
                null,
                false,
                0L
        );
        when(attendanceService.clockOut(EMPLOYEE_ID, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-out")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockOut").exists());
    }

    @Test
    @DisplayName("PATCH /api/attendance/records/{id}/memo は200を返す")
    void updateMemo_validRequest_returns200() throws Exception {
        // Arrange
        var recordId = UUID.randomUUID();
        var response = new AttendanceRecordResponse(
                recordId,
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "修正後メモ",
                null,
                false,
                1L
        );
        when(attendanceService.updateMemo(recordId, EMPLOYEE_ID, "修正後メモ", null, 0L))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/attendance/records/{id}/memo", recordId)
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType("application/json")
                        .content("{\"clockInMemo\":\"修正後メモ\",\"clockOutMemo\":null,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockInMemo").value("修正後メモ"));
    }

    @Test
    @DisplayName("PATCH memo は201文字メモで400を返す")
    void updateMemo_memoOver200_returns400() throws Exception {
        // Arrange
        var recordId = UUID.randomUUID();
        var longMemo = "あ".repeat(201);

        // Act & Assert
        mockMvc.perform(patch("/api/attendance/records/{id}/memo", recordId)
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType("application/json")
                        .content("{\"clockInMemo\":\"" + longMemo + "\",\"version\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/attendance/today は200を返す")
    void getTodayStatus_validRequest_returns200() throws Exception {
        // Arrange
        var response = new TodayStatusResponse(AttendanceStatus.NOT_CLOCKED_IN, List.of());
        when(attendanceService.getTodayStatus(EMPLOYEE_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/attendance/today")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CLOCKED_IN"))
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    @DisplayName("GET /api/attendance/history は200を返す")
    void getHistory_validRequest_returns200() throws Exception {
        // Arrange
        var dailyResponse = new DailyAttendanceResponse(
                LocalDate.of(2025, 1, 15),
                List.of(),
                540,
                60,
                480,
                0
        );
        var summary = new MonthlySummaryResponse(1, 480, 0, 22);
        var response = new AttendanceHistoryResponse("2025-01", List.of(dailyResponse), summary);
        when(attendanceService.getHistory(EMPLOYEE_ID, "2025-01")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/attendance/history")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .param("month", "2025-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2025-01"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.summary.workDays").value(1));
    }
}
