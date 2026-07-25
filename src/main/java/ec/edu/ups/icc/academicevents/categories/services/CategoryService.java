package ec.edu.ups.icc.academicevents.categories.services;

import ec.edu.ups.icc.academicevents.categories.dtos.CategoryResponse;
import ec.edu.ups.icc.academicevents.categories.entities.CategoryEntity;
import ec.edu.ups.icc.academicevents.categories.repositories.CategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(
            CategoryRepository categoryRepository
    ) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> findAllActive() {

        return categoryRepository
                .findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(
            CategoryEntity category
    ) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}