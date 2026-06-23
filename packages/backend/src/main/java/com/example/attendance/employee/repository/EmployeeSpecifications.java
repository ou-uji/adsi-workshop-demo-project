package com.example.attendance.employee.repository;

import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> hasDepartmentId(UUID departmentId) {
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> hasRole(Role role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<Employee> isNotRetired() {
        return (root, query, cb) -> cb.isNull(root.get("retireDate"));
    }
}
