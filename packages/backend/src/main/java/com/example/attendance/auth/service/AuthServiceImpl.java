package com.example.attendance.auth.service;

import com.example.attendance.auth.dto.AuthUserResponse;
import com.example.attendance.common.config.security.EmployeeUserDetails;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final EmployeeRepository employeeRepository;

    public AuthServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public AuthUserResponse getCurrentUser() {
        EmployeeUserDetails userDetails = (EmployeeUserDetails) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

        Employee employee = employeeRepository.findById(userDetails.getEmployeeId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Employee not found: " + userDetails.getEmployeeId()));

        return AuthUserResponse.from(employee);
    }
}
