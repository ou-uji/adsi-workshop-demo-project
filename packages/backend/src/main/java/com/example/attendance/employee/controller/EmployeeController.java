package com.example.attendance.employee.controller;

import com.example.attendance.employee.dto.EmployeeCreateRequest;
import com.example.attendance.employee.dto.EmployeeResponse;
import com.example.attendance.employee.dto.EmployeeUpdateRequest;
import com.example.attendance.employee.dto.ManagerRequest;
import com.example.attendance.employee.dto.RetireRequest;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public Page<EmployeeResponse> findAll(
            Pageable pageable,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "false") boolean includeRetired) {
        return employeeService.findAll(pageable, departmentId, role, includeRetired);
    }

    @GetMapping("/{id}")
    public EmployeeResponse findById(@PathVariable UUID id) {
        return employeeService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/retire")
    public EmployeeResponse retire(@PathVariable UUID id,
                                   @Valid @RequestBody RetireRequest request) {
        return employeeService.retire(id, request);
    }

    @PatchMapping("/{id}/manager")
    public EmployeeResponse setManager(@PathVariable UUID id,
                                       @Valid @RequestBody ManagerRequest request) {
        return employeeService.setManager(id, request);
    }
}
