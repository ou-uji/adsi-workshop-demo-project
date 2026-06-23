package com.example.attendance.department.repository;

import com.example.attendance.common.config.JpaAuditingConfig;
import com.example.attendance.department.entity.Department;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository repository;

    @Test
    @DisplayName("部署を保存して取得できる")
    void save_validDepartment_canBeRetrieved() {
        // Arrange
        var department = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name("開発部")
            .build();

        // Act
        var saved = repository.save(department);
        var found = repository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("開発部");
    }

    @Test
    @DisplayName("同名の部署を保存すると一意制約違反になる")
    void save_duplicateName_throwsDataIntegrityViolation() {
        // Arrange
        var department1 = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name("開発部")
            .build();
        repository.saveAndFlush(department1);

        var department2 = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name("開発部")
            .build();

        // Act & Assert
        assertThatThrownBy(() -> repository.saveAndFlush(department2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsByNameで既存の部署名を検索するとtrueが返る")
    void existsByName_existingName_returnsTrue() {
        // Arrange
        var department = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name("営業部")
            .build();
        repository.saveAndFlush(department);

        // Act
        var exists = repository.existsByName("営業部");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndIdNotで自分以外の同名部署がない場合falseが返る")
    void existsByNameAndIdNot_sameEntityName_returnsFalse() {
        // Arrange
        var department = Department.builder()
            .id(UuidCreator.getTimeOrderedEpoch())
            .name("総務部")
            .build();
        var saved = repository.saveAndFlush(department);

        // Act
        var exists = repository.existsByNameAndIdNot("総務部", saved.getId());

        // Assert
        assertThat(exists).isFalse();
    }
}
