package com.example.attendance.department.controller;

import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.department.dto.DepartmentRequest;
import com.example.attendance.department.dto.DepartmentResponse;
import com.example.attendance.department.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = DepartmentController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(DepartmentControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class DepartmentControllerTest {

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
    private DepartmentService departmentService;

    @Test
    @DisplayName("GET /api/departments で部署一覧が200で返る")
    void findAll_existingDepartments_returns200WithList() throws Exception {
        // Arrange
        var dept1 = new DepartmentResponse(UUID.randomUUID(), "開発部");
        var dept2 = new DepartmentResponse(UUID.randomUUID(), "営業部");
        when(departmentService.findAll()).thenReturn(List.of(dept1, dept2));

        // Act & Assert
        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("開発部"))
            .andExpect(jsonPath("$[1].name").value("営業部"));
    }

    @Test
    @DisplayName("POST /api/departments で部署が201で登録される")
    void create_validRequest_returns201WithDepartment() throws Exception {
        // Arrange
        var id = UUID.randomUUID();
        var response = new DepartmentResponse(id, "開発部");
        when(departmentService.create(any(DepartmentRequest.class))).thenReturn(response);

        var request = new DepartmentRequest("開発部");

        // Act & Assert
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.name").value("開発部"));
    }

    @Test
    @DisplayName("POST /api/departments でバリデーションエラー時に400が返る")
    void create_emptyName_returns400() throws Exception {
        // Arrange
        var request = new DepartmentRequest("");

        // Act & Assert
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/departments/{id} で部署が200で更新される")
    void update_validRequest_returns200WithUpdatedDepartment() throws Exception {
        // Arrange
        var id = UUID.randomUUID();
        var response = new DepartmentResponse(id, "技術部");
        when(departmentService.update(eq(id), any(DepartmentRequest.class))).thenReturn(response);

        var request = new DepartmentRequest("技術部");

        // Act & Assert
        mockMvc.perform(put("/api/departments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.name").value("技術部"));
    }
}
