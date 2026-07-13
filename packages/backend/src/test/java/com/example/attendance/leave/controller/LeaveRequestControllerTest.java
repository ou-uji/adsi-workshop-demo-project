package com.example.attendance.leave.controller;

import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.leave.dto.LeaveRequestResponse;
import com.example.attendance.leave.dto.PendingLeaveRequestResponse;
import com.example.attendance.leave.entity.LeaveStatus;
import com.example.attendance.leave.service.LeaveRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = LeaveRequestController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(LeaveRequestControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class LeaveRequestControllerTest {

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
    private LeaveRequestService leaveRequestService;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LEAVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    @DisplayName("POST /api/leave-requests は201を返す")
    void create_validRequest_returns201() throws Exception {
        // Arrange
        var response = new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎", null, null,
                LocalDate.of(2025, 10, 20),
                "私用のため", LeaveStatus.PENDING, null, 0L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.create(eq(EMPLOYEE_ID), any())).thenReturn(response);

        var body = """
                {
                    "targetDate": "2025-10-20",
                    "reason": "私用のため"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("私用のため"));
    }

    @Test
    @DisplayName("POST /api/leave-requests は理由が空だと400を返す")
    void create_blankReason_returns400() throws Exception {
        var body = """
                {
                    "targetDate": "2025-10-20",
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/leave-requests はtargetDate欠落だと400を返す")
    void create_missingTargetDate_returns400() throws Exception {
        var body = """
                {
                    "reason": "私用のため"
                }
                """;

        mockMvc.perform(post("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/leave-requests は理由501文字だと400を返す")
    void create_tooLongReason_returns400() throws Exception {
        var reason = "あ".repeat(501);
        var body = """
                {
                    "targetDate": "2025-10-20",
                    "reason": "%s"
                }
                """.formatted(reason);

        mockMvc.perform(post("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/leave-requests は理由500文字ちょうどだと201を返す")
    void create_maxLengthReason_returns201() throws Exception {
        var reason = "あ".repeat(500);
        var response = new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎", null, null,
                LocalDate.of(2025, 10, 20),
                reason, LeaveStatus.PENDING, null, 0L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.create(eq(EMPLOYEE_ID), any())).thenReturn(response);

        var body = """
                {
                    "targetDate": "2025-10-20",
                    "reason": "%s"
                }
                """.formatted(reason);

        mockMvc.perform(post("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/leave-requests は200と申請一覧を返す")
    void findByRequester_returns200() throws Exception {
        // Arrange
        var response = new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎", null, null,
                LocalDate.of(2025, 10, 20),
                "私用のため", LeaveStatus.PENDING, null, 0L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.findByRequester(eq(EMPLOYEE_ID), any()))
                .thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/leave-requests")
                        .param("requesterId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/leave-requests/pending は200を返す")
    void findPending_returns200() throws Exception {
        // Arrange
        var response = new PendingLeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎",
                LocalDate.of(2025, 10, 20),
                "私用のため", 0L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.findPending(MANAGER_ID)).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/leave-requests/pending")
                        .param("managerId", MANAGER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterName").value("田中太郎"));
    }

    @Test
    @DisplayName("PATCH /api/leave-requests/{id}/approve は200を返す")
    void approve_returns200() throws Exception {
        // Arrange
        var response = new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎",
                MANAGER_ID, "佐藤次郎",
                LocalDate.of(2025, 10, 20),
                "私用のため", LeaveStatus.APPROVED, null, 1L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.approve(LEAVE_ID, MANAGER_ID, 0L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/leave-requests/{id}/approve", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/leave-requests/{id}/reject は200を返す")
    void reject_returns200() throws Exception {
        // Arrange
        var response = new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "田中太郎",
                MANAGER_ID, "佐藤次郎",
                LocalDate.of(2025, 10, 20),
                "私用のため", LeaveStatus.REJECTED, "業務都合", 1L,
                Instant.parse("2025-10-01T00:00:00Z")
        );
        when(leaveRequestService.reject(eq(LEAVE_ID), eq(MANAGER_ID), eq("業務都合"), eq(0L)))
                .thenReturn(response);

        var body = """
                {
                    "reason": "業務都合",
                    "version": 0
                }
                """;

        // Act & Assert
        mockMvc.perform(patch("/api/leave-requests/{id}/reject", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("業務都合"));
    }

    @Test
    @DisplayName("PATCH /api/leave-requests/{id}/reject は理由が空だと400を返す")
    void reject_blankReason_returns400() throws Exception {
        var body = """
                {
                    "reason": "",
                    "version": 0
                }
                """;

        mockMvc.perform(patch("/api/leave-requests/{id}/reject", LEAVE_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
