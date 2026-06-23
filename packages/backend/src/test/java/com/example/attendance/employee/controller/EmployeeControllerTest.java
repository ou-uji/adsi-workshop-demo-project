package com.example.attendance.employee.controller;

import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.employee.dto.EmployeeResponse;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = EmployeeController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(EmployeeControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class EmployeeControllerTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    private final UUID employeeId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();

    private EmployeeResponse sampleResponse() {
        return new EmployeeResponse(
            employeeId, "田中太郎", "tanaka@example.com",
            departmentId, "Engineering", Role.EMPLOYEE,
            false, LocalDate.of(2024, 4, 1), null);
    }

    @Test
    @DisplayName("GET /api/employees: 200とページネーション結果を返す")
    void findAll_returnsPagedResult() throws Exception {
        // Arrange
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(sampleResponse()), pageable, 1);
        when(employeeService.findAll(any(), isNull(), isNull(), eq(false))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/employees"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("田中太郎"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/employees/{id}: 200と社員情報を返す")
    void findById_returnsEmployee() throws Exception {
        // Arrange
        when(employeeService.findById(employeeId)).thenReturn(sampleResponse());

        // Act & Assert
        mockMvc.perform(get("/api/employees/{id}", employeeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("田中太郎"))
            .andExpect(jsonPath("$.email").value("tanaka@example.com"));
    }

    @Test
    @DisplayName("POST /api/employees: 201と作成された社員情報を返す")
    void create_validRequest_returns201() throws Exception {
        // Arrange
        when(employeeService.create(any())).thenReturn(sampleResponse());
        var body = """
            {
                "name": "田中太郎",
                "email": "tanaka@example.com",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.formatted(departmentId);

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("田中太郎"));
    }

    @Test
    @DisplayName("POST /api/employees: バリデーションエラーで400を返す")
    void create_invalidRequest_returns400() throws Exception {
        // Arrange
        var body = """
            {
                "name": "",
                "email": "invalid",
                "password": "short",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/employees/{id}: 200と更新された社員情報を返す")
    void update_validRequest_returns200() throws Exception {
        // Arrange
        var updatedResponse = new EmployeeResponse(
            employeeId, "佐藤花子", "sato@example.com",
            departmentId, "Engineering", Role.ADMIN,
            false, LocalDate.of(2023, 4, 1), null);
        when(employeeService.update(eq(employeeId), any())).thenReturn(updatedResponse);
        var body = """
            {
                "name": "佐藤花子",
                "email": "sato@example.com",
                "departmentId": "%s",
                "role": "ADMIN",
                "hireDate": "2023-04-01"
            }
            """.formatted(departmentId);

        // Act & Assert
        mockMvc.perform(put("/api/employees/{id}", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("佐藤花子"));
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/retire: 200と退職処理された社員情報を返す")
    void retire_validRequest_returns200() throws Exception {
        // Arrange
        var retiredResponse = new EmployeeResponse(
            employeeId, "田中太郎", "tanaka@example.com",
            departmentId, "Engineering", Role.EMPLOYEE,
            false, LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31));
        when(employeeService.retire(eq(employeeId), any())).thenReturn(retiredResponse);
        var body = """
            {
                "retireDate": "2025-03-31"
            }
            """;

        // Act & Assert
        mockMvc.perform(patch("/api/employees/{id}/retire", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retireDate").value("2025-03-31"));
    }

    @Test
    @DisplayName("PATCH /api/employees/{id}/manager: 200と上長設定された社員情報を返す")
    void setManager_validRequest_returns200() throws Exception {
        // Arrange
        var managerResponse = new EmployeeResponse(
            employeeId, "田中太郎", "tanaka@example.com",
            departmentId, "Engineering", Role.EMPLOYEE,
            true, LocalDate.of(2024, 4, 1), null);
        when(employeeService.setManager(eq(employeeId), any())).thenReturn(managerResponse);
        var body = """
            {
                "isManager": true
            }
            """;

        // Act & Assert
        mockMvc.perform(patch("/api/employees/{id}/manager", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isManager").value(true));
    }
}
