package ec.edu.ups.icc.academicevents.categories.repositories;

import ec.edu.ups.icc.academicevents.categories.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByActiveTrueOrderByNameAsc();

    Optional<CategoryEntity> findByIdAndActiveTrue(Long id);
}
