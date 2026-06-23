package com.example.attendance.employee.service;

import com.example.attendance.department.repository.DepartmentRepository;
import com.example.attendance.employee.dto.EmployeeCreateRequest;
import com.example.attendance.employee.dto.EmployeeResponse;
import com.example.attendance.employee.dto.EmployeeUpdateRequest;
import com.example.attendance.employee.dto.ManagerRequest;
import com.example.attendance.employee.dto.RetireRequest;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import com.example.attendance.employee.repository.EmployeeSpecifications;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> findAll(Pageable pageable, UUID departmentId, Role role, boolean includeRetired) {
        Specification<Employee> spec = Specification.where(null);

        if (departmentId != null) {
            spec = spec.and(EmployeeSpecifications.hasDepartmentId(departmentId));
        }
        if (role != null) {
            spec = spec.and(EmployeeSpecifications.hasRole(role));
        }
        if (!includeRetired) {
            spec = spec.and(EmployeeSpecifications.isNotRetired());
        }

        return employeeRepository.findAll(spec, pageable)
            .map(EmployeeResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(UUID id) {
        var employee = findEmployeeOrThrow(id);
        return EmployeeResponse.from(employee);
    }

    @Override
    public EmployeeResponse create(EmployeeCreateRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        var department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Department with id '%s' was not found".formatted(request.departmentId())));

        var employee = Employee.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .department(department)
            .role(request.role())
            .isManager(false)
            .hireDate(request.hireDate())
            .build();

        var saved = employeeRepository.save(employee);
        log.info("Employee created: id={}, email={}", saved.getId(), saved.getEmail());
        return EmployeeResponse.from(saved);
    }

    @Override
    public EmployeeResponse update(UUID id, EmployeeUpdateRequest request) {
        var employee = findEmployeeOrThrow(id);

        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        var department = departmentRepository.findById(request.departmentId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Department with id '%s' was not found".formatted(request.departmentId())));

        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setDepartment(department);
        employee.setRole(request.role());
        employee.setHireDate(request.hireDate());

        var saved = employeeRepository.save(employee);
        log.info("Employee updated: id={}", saved.getId());
        return EmployeeResponse.from(saved);
    }

    @Override
    public EmployeeResponse retire(UUID id, RetireRequest request) {
        var employee = findEmployeeOrThrow(id);

        if (employee.getRetireDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is already retired");
        }

        employee.setRetireDate(request.retireDate());
        var saved = employeeRepository.save(employee);
        log.info("Employee retired: id={}, retireDate={}", saved.getId(), saved.getRetireDate());
        return EmployeeResponse.from(saved);
    }

    @Override
    public EmployeeResponse setManager(UUID id, ManagerRequest request) {
        var employee = findEmployeeOrThrow(id);

        if (request.isManager()) {
            var departmentId = employee.getDepartment().getId();
            if (employeeRepository.existsByDepartmentIdAndIsManagerTrueAndIdNot(departmentId, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department already has a manager");
            }
        }

        employee.setManager(request.isManager());
        var saved = employeeRepository.save(employee);
        log.info("Employee manager status changed: id={}, isManager={}", saved.getId(), saved.isManager());
        return EmployeeResponse.from(saved);
    }

    private Employee findEmployeeOrThrow(UUID id) {
        return employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "Employee with id '%s' was not found".formatted(id)));
    }
}
