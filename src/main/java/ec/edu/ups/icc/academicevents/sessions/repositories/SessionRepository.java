package ec.edu.ups.icc.academicevents.sessions.repositories;

import ec.edu.ups.icc.academicevents.sessions.entities.SessionEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface SessionRepository
        extends JpaRepository<SessionEntity, Long> {

    @EntityGraph(attributePaths = {"event"})
    Page<SessionEntity> findAllByEvent_Id(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"event"})
    Optional<SessionEntity> findById(Long id);

    @EntityGraph(attributePaths = {"event"})
    Page<SessionEntity> findAllByStartAtAfter(OffsetDateTime moment, Pageable pageable);
}