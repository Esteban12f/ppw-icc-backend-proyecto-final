package ec.edu.ups.icc.academicevents.sessions.repositories;

import ec.edu.ups.icc.academicevents.sessions.entities.SessionEntity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<SessionEntity, Long> {

    @EntityGraph(
            attributePaths = {
                    "event"
            }
    )
    List<SessionEntity>
    findAllByEvent_IdOrderByStartAtAsc(Long eventId);

    @EntityGraph(
            attributePaths = {
                    "event"
            }
    )
    Optional<SessionEntity>
    findById(Long id);

    @EntityGraph(
            attributePaths = {
                    "event"
            }
    )
    List<SessionEntity>
    findAllByStartAtAfterOrderByStartAtAsc(
            OffsetDateTime moment
    );
}