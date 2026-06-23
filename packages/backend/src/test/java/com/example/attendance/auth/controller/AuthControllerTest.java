package com.example.attendance.auth.controller;

import com.example.attendance.auth.dto.AuthUserResponse;
import com.example.attendance.auth.service.AuthService;
import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.employee.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(AuthControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/me").authenticated()
                    .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpStatus.UNAUTHORIZED.value()))
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("認証済みユーザーがGET /api/auth/meにアクセスするとユーザー情報が返される")
    @WithMockUser(username = "tanaka@example.com", roles = "ADMIN")
    void getCurrentUser_authenticated_returnsUserInfo() throws Exception {
        // Arrange
        var employeeId = UUID.randomUUID();
        var departmentId = UUID.randomUUID();
        var response = new AuthUserResponse(
            employeeId, "田中太郎", "tanaka@example.com",
            departmentId, "開発部", Role.ADMIN, true);
        when(authService.getCurrentUser()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("田中太郎"))
            .andExpect(jsonPath("$.email").value("tanaka@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.isManager").value(true));
    }

    @Test
    @DisplayName("未認証ユーザーがGET /api/auth/meにアクセスすると401が返される")
    void getCurrentUser_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }
}
