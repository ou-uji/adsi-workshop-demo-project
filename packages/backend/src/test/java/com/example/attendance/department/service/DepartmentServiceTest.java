package com.example.attendance.department.service;

import com.example.attendance.department.dto.DepartmentRequest;
import com.example.attendance.department.entity.Department;
import com.example.attendance.department.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repository;

    private DepartmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DepartmentServiceImpl(repository);
    }

    @Test
    @DisplayName("全部署がリストで返る")
    void findAll_existingDepartments_returnsList() {
        // Arrange
        var dept1 = Department.builder().id(UUID.randomUUID()).name("開発部").build();
        var dept2 = Department.builder().id(UUID.randomUUID()).name("営業部").build();
        when(repository.findAll()).thenReturn(List.of(dept1, dept2));

        // Act
        var result = service.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("開発部");
        assertThat(result.get(1).name()).isEqualTo("営業部");
    }

    @Test
    @DisplayName("正常に部署を登録できる")
    void create_validRequest_returnsDepartment() {
        // Arrange
        var request = new DepartmentRequest("開発部");
        when(repository.existsByName("開発部")).thenReturn(false);
        when(repository.save(any(Department.class))).thenAnswer(invocation -> {
            Department dept = invocation.getArgument(0);
            return Department.builder()
                .id(dept.getId())
                .name(dept.getName())
                .build();
        });

        // Act
        var result = service.create(request);

        // Assert
        assertThat(result.name()).isEqualTo("開発部");
        assertThat(result.id()).isNotNull();
    }

    @Test
    @DisplayName("名前が重複する部署を登録すると409エラーになる")
    void create_duplicateName_throwsConflict() {
        // Arrange
        var request = new DepartmentRequest("開発部");
        when(repository.existsByName("開発部")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.create(request))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                var rse = (ResponseStatusException) ex;
                assertThat(rse.getStatusCode().value()).isEqualTo(409);
            });
    }

    @Test
    @DisplayName("部署名が正常に更新される")
    void update_existingDepartment_updatesName() {
        // Arrange
        var id = UUID.randomUUID();
        var existing = Department.builder().id(id).name("開発部").build();
        var request = new DepartmentRequest("技術部");
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.existsByNameAndIdNot("技術部", id)).thenReturn(false);
        when(repository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = service.update(id, request);

        // Assert
        assertThat(result.name()).isEqualTo("技術部");
        assertThat(result.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("存在しないIDで更新するとEntityNotFoundExceptionが発生する")
    void update_nonExistingId_throwsEntityNotFound() {
        // Arrange
        var id = UUID.randomUUID();
        var request = new DepartmentRequest("開発部");
        when(repository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.update(id, request))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
