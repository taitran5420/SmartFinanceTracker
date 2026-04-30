package com.seap.smartfinancetracker.category.repository;

import com.seap.smartfinancetracker.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID userId);

    List<Category> findByUserIdIsNull();

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);
}
