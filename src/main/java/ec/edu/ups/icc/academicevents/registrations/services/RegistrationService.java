package ec.edu.ups.icc.academicevents.registrations.services;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.events.entities.EventStatus;
import ec.edu.ups.icc.academicevents.events.repositories.EventRepository;
import ec.edu.ups.icc.academicevents.registrations.dtos.RegistrationResponse;
import ec.edu.ups.icc.academicevents.registrations.dtos.RegistrationStatusRequest;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationEntity;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationStatus;
import ec.edu.ups.icc.academicevents.registrations.repositories.RegistrationRepository;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;
import jakarta.persistence.EntityManager;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final EntityManager entityManager;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            EntityManager entityManager
    ) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public RegistrationResponse create(
            Long eventId,
            Authentication authentication
    ) {
        ensureParticipant(authentication);

        UserEntity participant =
                findAuthenticatedUser(authentication);

        EventEntity event = findActiveEvent(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw badRequest(
                    "El evento no acepta inscripciones "
                            + "en su estado actual"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (now.isBefore(event.getRegistrationStartAt())
                || now.isAfter(event.getRegistrationEndAt())) {

            throw badRequest(
                    "El periodo de inscripciones "
                            + "para este evento no está activo"
            );
        }

        if (registrationRepository
                .existsByEvent_IdAndParticipant_Id(
                        event.getId(),
                        participant.getId()
                )) {

            throw conflict(
                    "Ya existe una inscripción de este "
                            + "usuario para este evento"
            );
        }

        RegistrationEntity registration =
                new RegistrationEntity();

        registration.setRegistrationCode(UUID.randomUUID());
        registration.setEvent(event);
        registration.setParticipant(participant);
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setStatusUpdatedAt(now);

        try {
            registrationRepository
                    .saveAndFlush(registration);

        } catch (DataIntegrityViolationException exception) {

            throw conflict(
                    "Ya existe una inscripción de este "
                            + "usuario para este evento"
            );
        }

        entityManager.refresh(registration);

        return toResponse(registration);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> findMine(
            Authentication authentication,
            Pageable pageable
    ) {
        UserEntity participant =
                findAuthenticatedUser(authentication);

        return registrationRepository
                .findAllByParticipant_Id(
                        participant.getId(),
                        pageable
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> findAllByEvent(
            Long eventId,
            Authentication authentication,
            Pageable pageable
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        EventEntity event = findActiveEvent(eventId);

        ensureOwnerOrAdmin(event, actor, authentication);

        return registrationRepository
                .findAllByEvent_Id(
                        event.getId(),
                        pageable
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RegistrationResponse findById(
            Long id,
            Authentication authentication
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        RegistrationEntity registration =
                findRegistration(id);

        ensureOwnerOrManagerOfEvent(
                registration,
                actor,
                authentication
        );

        return toResponse(registration);
    }

    @Transactional
    public RegistrationResponse changeStatus(
            Long id,
            RegistrationStatusRequest request,
            Authentication authentication
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        RegistrationEntity registration =
                findRegistration(id);

        EventEntity event = registration.getEvent();

        ensureOwnerOrAdmin(event, actor, authentication);

        RegistrationStatus currentStatus =
                registration.getStatus();

        RegistrationStatus newStatus =
                request.getStatus();

        if (currentStatus
                == RegistrationStatus.CANCELLED) {

            throw badRequest(
                    "La inscripción está cancelada "
                            + "y no puede cambiar de estado"
            );
        }

        if (newStatus == RegistrationStatus.CANCELLED) {

            throw badRequest(
                    "Use DELETE /registrations/{id} "
                            + "para cancelar una inscripción"
            );
        }

        if (newStatus == RegistrationStatus.PENDING) {

            throw badRequest(
                    "No se puede volver el estado "
                            + "a PENDING"
            );
        }

        adjustCapacity(
                event,
                currentStatus,
                newStatus
        );

        registration.setStatus(newStatus);
        registration.setStatusUpdatedAt(
                OffsetDateTime.now()
        );

        if (newStatus == RegistrationStatus.CONFIRMED) {

            registration.setConfirmedAt(
                    OffsetDateTime.now()
            );

        } else {

            registration.setConfirmedAt(null);
        }

        eventRepository.saveAndFlush(event);
        registrationRepository
                .saveAndFlush(registration);

        entityManager.refresh(registration);
        entityManager.refresh(event);

        return toResponse(registration);
    }

    @Transactional
    public void cancel(
            Long id,
            Authentication authentication
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        RegistrationEntity registration =
                findRegistration(id);

        boolean isAdmin =
                hasRole(authentication, "ADMIN");

        boolean isOwner =
                registration.getParticipant()
                        .getId()
                        .equals(actor.getId());

        if (!isAdmin && !isOwner) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el participante propietario "
                            + "puede cancelar esta inscripción"
            );
        }

        RegistrationStatus currentStatus =
                registration.getStatus();

        if (currentStatus
                == RegistrationStatus.CANCELLED) {

            throw badRequest(
                    "La inscripción ya está cancelada"
            );
        }

        EventEntity event = registration.getEvent();

        adjustCapacity(
                event,
                currentStatus,
                RegistrationStatus.CANCELLED
        );

        registration.setStatus(
                RegistrationStatus.CANCELLED
        );

        registration.setStatusUpdatedAt(
                OffsetDateTime.now()
        );

        registration.setCancelledAt(
                OffsetDateTime.now()
        );

        eventRepository.saveAndFlush(event);
        registrationRepository
                .saveAndFlush(registration);
    }

    private void adjustCapacity(
            EventEntity event,
            RegistrationStatus oldStatus,
            RegistrationStatus newStatus
    ) {
        boolean oldConsumes =
                oldStatus == RegistrationStatus.CONFIRMED;

        boolean newConsumes =
                newStatus == RegistrationStatus.CONFIRMED;

        if (!oldConsumes && newConsumes) {

            if (event.getAvailableCapacity() <= 0) {

                throw conflict(
                        "No hay cupos disponibles "
                                + "para este evento"
                );
            }

            event.setAvailableCapacity(
                    event.getAvailableCapacity() - 1
            );

        } else if (oldConsumes && !newConsumes) {

            int restored =
                    event.getAvailableCapacity() + 1;

            event.setAvailableCapacity(
                    Math.min(
                            restored,
                            event.getCapacity()
                    )
            );
        }
    }

    private RegistrationEntity findRegistration(
            Long id
    ) {
        return registrationRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Inscripción no encontrada"
                        )
                );
    }

    private EventEntity findActiveEvent(Long id) {

        return eventRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Evento no encontrado"
                        )
                );
    }

    private UserEntity findAuthenticatedUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "El usuario autenticado "
                                        + "no existe"
                        )
                );
    }

    private void ensureParticipant(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        if (!hasRole(authentication, "PARTICIPANT")) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los participantes pueden "
                            + "inscribirse a eventos"
            );
        }
    }

    private void ensureOwnerOrAdmin(
            EventEntity event,
            UserEntity actor,
            Authentication authentication
    ) {
        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        Long organizerId =
                event.getOrganizer().getId();

        if (!organizerId.equals(actor.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el organizador propietario "
                            + "del evento puede gestionar "
                            + "estas inscripciones"
            );
        }
    }

    private void ensureOwnerOrManagerOfEvent(
            RegistrationEntity registration,
            UserEntity actor,
            Authentication authentication
    ) {
        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        boolean isParticipantOwner =
                registration.getParticipant()
                        .getId()
                        .equals(actor.getId());

        if (isParticipantOwner) {
            return;
        }

        Long organizerId = registration
                .getEvent()
                .getOrganizer()
                .getId();

        if (!organizerId.equals(actor.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para ver "
                            + "esta inscripción"
            );
        }
    }

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {
        String roleAuthority = "ROLE_" + role;

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equalsIgnoreCase(role)
                                || authority.equalsIgnoreCase(
                                        roleAuthority
                                )
                );
    }

    private RegistrationResponse toResponse(
            RegistrationEntity registration
    ) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getRegistrationCode(),
                registration.getEvent().getId(),
                registration.getEvent().getTitle(),
                registration.getParticipant().getId(),
                registration.getParticipant()
                        .getFullName(),
                registration.getParticipant()
                        .getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getStatusUpdatedAt(),
                registration.getConfirmedAt(),
                registration.getCancelledAt(),
                registration.getVersion()
        );
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}