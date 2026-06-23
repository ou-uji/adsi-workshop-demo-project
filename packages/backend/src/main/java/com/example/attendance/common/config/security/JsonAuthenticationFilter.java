package com.example.attendance.common.config.security;

import com.example.attendance.auth.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

public class JsonAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new AuthenticationServiceException(
                "Authentication method not supported: " + request.getMethod());
        }

        try {
            LoginRequest loginRequest = objectMapper.readValue(
                request.getInputStream(), LoginRequest.class);

            String email = loginRequest.email() != null ? loginRequest.email().trim() : "";
            String password = loginRequest.password() != null ? loginRequest.password() : "";

            var authToken = new UsernamePasswordAuthenticationToken(email, password);
            return getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new AuthenticationServiceException("Failed to parse login request", e);
        }
    }
}
