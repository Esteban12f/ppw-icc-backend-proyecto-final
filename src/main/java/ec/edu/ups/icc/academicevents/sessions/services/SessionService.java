package ec.edu.ups.icc.academicevents.sessions.services;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.events.entities.EventStatus;
import ec.edu.ups.icc.academicevents.events.repositories.EventRepository;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.entities.SessionEntity;
import ec.edu.ups.icc.academicevents.sessions.repositories.SessionRepository;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

import jakarta.persistence.EntityManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public SessionService(
            SessionRepository sessionRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            EntityManager entityManager
    ) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> findAllByEvent(Long eventId, Pageable pageable) {
        EventEntity event = findActiveEvent(eventId);
        return sessionRepository
                .findAllByEvent_Id(event.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionResponse findById(Long id) {
        SessionEntity session = findSession(id);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> findUpcoming(Pageable pageable) {
        return sessionRepository
                .findAllByStartAtAfter(OffsetDateTime.now(), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public SessionResponse create(Long eventId, SessionRequest request, Authentication authentication) {
        ensureManager(authentication);
        UserEntity actor = findAuthenticatedUser(authentication);
        EventEntity event = findActiveEvent(eventId);
        ensureOwnerOrAdmin(event, actor, authentication);
        validateRequest(request, event);

        SessionEntity session = new SessionEntity();
        session.setEvent(event);
        applyRequest(session, request);

        try {
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Ya existe una sesion con ese titulo y fecha de inicio para este evento");
        }

        entityManager.refresh(session);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse update(Long id, SessionRequest request, Authentication authentication) {
        ensureManager(authentication);
        UserEntity actor = findAuthenticatedUser(authentication);
        SessionEntity session = findSession(id);
        EventEntity event = session.getEvent();
        ensureOwnerOrAdmin(event, actor, authentication);
        validateRequest(request, event);
        applyRequest(session, request);

        try {
            sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Ya existe una sesion con ese titulo y fecha de inicio para este evento");
        }

        entityManager.refresh(session);
        return toResponse(session);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        ensureManager(authentication);
        UserEntity actor = findAuthenticatedUser(authentication);
        SessionEntity session = findSession(id);
        ensureOwnerOrAdmin(session.getEvent(), actor, authentication);
        sessionRepository.delete(session);
    }

    private void applyRequest(SessionEntity session, SessionRequest request) {
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setStartAt(request.getStartAt());
        session.setEndAt(request.getEndAt());
        session.setLocation(request.getLocation());
        session.setVirtualUrl(request.getVirtualUrl());
    }

    private void validateRequest(SessionRequest request, EventEntity event) {
        OffsetDateTime startAt = request.getStartAt();
        OffsetDateTime endAt = request.getEndAt();

        if (event.getStatus() == EventStatus.FINISHED
                || event.getStatus() == EventStatus.CANCELLED) {
            throw badRequest("No se pueden gestionar sesiones de un evento finalizado o cancelado");
        }

        if (startAt == null || endAt == null) {
            throw badRequest("Las fechas de inicio y fin son obligatorias");
        }

        if (!startAt.isBefore(endAt)) {
            throw badRequest("El inicio de la sesion debe ser anterior a su finalizacion");
        }

        if (startAt.isBefore(event.getStartAt()) || endAt.isAfter(event.getEndAt())) {
            throw badRequest(
                    "La sesion debe estar dentro del periodo del evento ("
                            + event.getStartAt() + " - " + event.getEndAt() + ")"
            );
        }
    }

    private EventEntity findActiveEvent(Long eventId) {
        return eventRepository
                .findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    private SessionEntity findSession(Long id) {
        return sessionRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesion no encontrada"));
    }

    private UserEntity findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String email = authentication.getName();

        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "El usuario autenticado no existe"
                ));
    }

    private void ensureManager(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        boolean allowed = hasRole(authentication, "ADMIN") || hasRole(authentication, "ORGANIZER");

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo administradores y organizadores pueden gestionar sesiones"
            );
        }
    }

    private void ensureOwnerOrAdmin(EventEntity event, UserEntity actor, Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        Long organizerId = event.getOrganizer().getId();

        if (!organizerId.equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el organizador propietario del evento puede gestionar sus sesiones"
            );
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        String roleAuthority = "ROLE_" + role;

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equalsIgnoreCase(role) || authority.equalsIgnoreCase(roleAuthority)
                );
    }

    private SessionResponse toResponse(SessionEntity session) {
        return new SessionResponse(
                session.getId(),
                session.getEvent().getId(),
                session.getEvent().getTitle(),
                session.getTitle(),
                session.getDescription(),
                session.getStartAt(),
                session.getEndAt(),
                session.getLocation(),
                session.getVirtualUrl(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}