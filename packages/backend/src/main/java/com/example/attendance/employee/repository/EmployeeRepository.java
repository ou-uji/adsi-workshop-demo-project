package com.example.attendance.employee.repository;

import com.example.attendance.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByDepartmentIdAndIsManagerTrue(UUID departmentId);

    boolean existsByDepartmentIdAndIsManagerTrueAndIdNot(UUID departmentId, UUID id);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByDepartmentId(UUID departmentId);
}
