package ec.edu.ups.icc.academicevents.registrations.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationEntity;

public interface RegistrationRepository
        extends JpaRepository<RegistrationEntity, Long> {

    @EntityGraph(attributePaths = {"event", "participant"})
    Optional<RegistrationEntity> findById(Long id);

    @EntityGraph(attributePaths = {"event", "participant"})
    Page<RegistrationEntity> findAllByParticipant_Id(
            Long participantId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"event", "participant"})
    Page<RegistrationEntity> findAllByEvent_Id(
            Long eventId,
            Pageable pageable
    );

    boolean existsByEvent_IdAndParticipant_Id(
            Long eventId,
            Long participantId
    );
    @EntityGraph(attributePaths = {"event", "participant"})
    List<RegistrationEntity> findAllByEvent_IdAndRegisteredAtBetweenOrderByRegisteredAtAsc(
            Long eventId,
            OffsetDateTime from,
            OffsetDateTime to
    );




}