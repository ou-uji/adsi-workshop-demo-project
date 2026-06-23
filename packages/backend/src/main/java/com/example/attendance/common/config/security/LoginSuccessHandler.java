package com.example.attendance.common.config.security;

import com.example.attendance.auth.dto.AuthUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    public LoginSuccessHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        EmployeeUserDetails userDetails = (EmployeeUserDetails) authentication.getPrincipal();

        var authUserResponse = new AuthUserResponse(
            userDetails.getEmployeeId(),
            userDetails.getEmployeeName(),
            userDetails.getUsername(),
            userDetails.getDepartmentId(),
            userDetails.getDepartmentName(),
            userDetails.getRole(),
            userDetails.isManager()
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), authUserResponse);
    }
}
