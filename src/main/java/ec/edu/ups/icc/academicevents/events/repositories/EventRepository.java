package ec.edu.ups.icc.academicevents.events.repositories;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository
        extends JpaRepository<EventEntity, Long> {

    @EntityGraph(
            attributePaths = {
                    "category",
                    "organizer"
            }
    )
    List<EventEntity>
    findAllByDeletedFalseOrderByStartAtAsc();

    @EntityGraph(
            attributePaths = {
                    "category",
                    "organizer"
            }
    )
    Optional<EventEntity>
    findByIdAndDeletedFalse(Long id);
}