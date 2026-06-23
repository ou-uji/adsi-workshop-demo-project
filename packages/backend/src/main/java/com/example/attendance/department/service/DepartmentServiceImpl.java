package com.example.attendance.department.service;

import com.example.attendance.department.dto.DepartmentRequest;
import com.example.attendance.department.dto.DepartmentResponse;
import com.example.attendance.department.entity.Department;
import com.example.attendance.department.repository.DepartmentRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentServiceImpl(DepartmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DepartmentResponse> findAll() {
        return repository.findAll().stream()
            .map(DepartmentResponse::from)
            .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (repository.existsByName(request.name())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "部署名「%s」は既に登録されています".formatted(request.name())
            );
        }

        var department = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name(request.name())
            .build();

        var saved = repository.save(department);
        log.info("部署を作成しました: id={}, name={}", saved.getId(), saved.getName());
        return DepartmentResponse.from(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(UUID id, DepartmentRequest request) {
        var department = repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                "Department with id '%s' was not found".formatted(id)
            ));

        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "部署名「%s」は既に登録されています".formatted(request.name())
            );
        }

        department.setName(request.name());
        var saved = repository.save(department);
        log.info("部署を更新しました: id={}, name={}", saved.getId(), saved.getName());
        return DepartmentResponse.from(saved);
    }
}
