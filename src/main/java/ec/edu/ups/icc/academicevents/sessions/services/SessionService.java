package ec.edu.ups.icc.academicevents.sessions.services;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.events.repositories.EventRepository;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.entities.SessionEntity;
import ec.edu.ups.icc.academicevents.sessions.repositories.SessionRepository;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

import jakarta.persistence.EntityManager;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

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
    public List<SessionResponse> findAllByEvent(Long eventId) {
        EventEntity event = findActiveEvent(eventId);

        return sessionRepository
                .findAllByEvent_IdOrderByStartAtAsc(event.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse findById(Long id) {
        SessionEntity session = findSession(id);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> findUpcoming() {
        return sessionRepository
                .findAllByStartAtAfterOrderByStartAtAsc(OffsetDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse create(
            Long eventId, SessionRequest request, Authentication authentication
    ) {
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
    public SessionResponse update(
            Long id, SessionRequest request, Authentication authentication
    ) {
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

}