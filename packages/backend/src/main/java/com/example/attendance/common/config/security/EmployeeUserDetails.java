package com.example.attendance.common.config.security;

import com.example.attendance.employee.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

public class EmployeeUserDetails extends User {

    public record EmployeeInfo(
            UUID employeeId,
            String employeeName,
            UUID departmentId,
            String departmentName,
            Role role,
            boolean manager) implements Serializable {
    }

    private final EmployeeInfo employeeInfo;

    public EmployeeUserDetails(
            String email,
            String password,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities,
            EmployeeInfo employeeInfo) {
        super(email, password, enabled, true, true, true, authorities);
        this.employeeInfo = employeeInfo;
    }

    public UUID getEmployeeId() {
        return employeeInfo.employeeId();
    }

    public String getEmployeeName() {
        return employeeInfo.employeeName();
    }

    public UUID getDepartmentId() {
        return employeeInfo.departmentId();
    }

    public String getDepartmentName() {
        return employeeInfo.departmentName();
    }

    public Role getRole() {
        return employeeInfo.role();
    }

    public boolean isManager() {
        return employeeInfo.manager();
    }
}
