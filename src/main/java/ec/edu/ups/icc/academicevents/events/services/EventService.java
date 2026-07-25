package ec.edu.ups.icc.academicevents.events.services;

import ec.edu.ups.icc.academicevents.categories.entities.CategoryEntity;
import ec.edu.ups.icc.academicevents.categories.repositories.CategoryRepository;
import ec.edu.ups.icc.academicevents.events.dtos.EventRequest;
import ec.edu.ups.icc.academicevents.events.dtos.EventResponse;
import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.events.entities.EventModality;
import ec.edu.ups.icc.academicevents.events.repositories.EventRepository;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    private final CategoryRepository categoryRepository;

    private final UserRepository userRepository;

    private final EntityManager entityManager;

    public EventService(
            EventRepository eventRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            EntityManager entityManager
    ) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {

        return eventRepository
                .findAllByDeletedFalseOrderByStartAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {

        EventEntity event = findActiveEvent(id);

        return toResponse(event);
    }

    @Transactional
    public EventResponse create(
            EventRequest request,
            Authentication authentication
    ) {
        ensureManager(authentication);

        UserEntity organizer =
                findAuthenticatedUser(authentication);

        CategoryEntity category =
                findActiveCategory(
                        request.getCategoryId()
                );

        validateRequest(request);

        EventEntity event = new EventEntity();

        event.setOrganizer(organizer);
        event.setCategory(category);
        event.setDeleted(false);

        applyRequest(
                event,
                request,
                true
        );

        eventRepository.saveAndFlush(event);

        entityManager.refresh(event);

        return toResponse(event);
    }

    @Transactional
    public EventResponse update(
            Long id,
            EventRequest request,
            Authentication authentication
    ) {
        ensureManager(authentication);

        UserEntity actor =
                findAuthenticatedUser(authentication);

        EventEntity event =
                findActiveEvent(id);

        ensureOwnerOrAdmin(
                event,
                actor,
                authentication
        );

        CategoryEntity category =
                findActiveCategory(
                        request.getCategoryId()
                );

        validateRequest(request);

        event.setCategory(category);

        applyRequest(
                event,
                request,
                false
        );

        eventRepository.saveAndFlush(event);

        entityManager.refresh(event);

        return toResponse(event);
    }

    @Transactional
    public void delete(
            Long id,
            Authentication authentication
    ) {
        ensureManager(authentication);

        UserEntity actor =
                findAuthenticatedUser(authentication);

        EventEntity event =
                findActiveEvent(id);

        ensureOwnerOrAdmin(
                event,
                actor,
                authentication
        );

        event.setDeleted(true);

        eventRepository.saveAndFlush(event);
    }

    private void applyRequest(
            EventEntity event,
            EventRequest request,
            boolean creation
    ) {
        String title = normalizeRequired(
                request.getTitle(),
                "El título es obligatorio"
        );

        String description = normalizeRequired(
                request.getDescription(),
                "La descripción es obligatoria"
        );

        String location =
                normalizeNullable(
                        request.getLocation()
                );

        String virtualUrl =
                normalizeNullable(
                        request.getVirtualUrl()
                );

        EventModality modality =
                request.getModality();

        switch (modality) {

            case PRESENTIAL -> {

                if (location == null) {
                    throw badRequest(
                            "Los eventos presenciales "
                                    + "requieren una ubicación"
                    );
                }

                virtualUrl = null;
            }

            case VIRTUAL -> {

                if (virtualUrl == null) {
                    throw badRequest(
                            "Los eventos virtuales "
                                    + "requieren una URL"
                    );
                }

                location = null;
            }

            case HYBRID -> {

                if (location == null) {
                    throw badRequest(
                            "Los eventos híbridos "
                                    + "requieren una ubicación"
                    );
                }

                if (virtualUrl == null) {
                    throw badRequest(
                            "Los eventos híbridos "
                                    + "requieren una URL"
                    );
                }
            }
        }

        int requestedCapacity =
                request.getCapacity();

        if (creation) {

            event.setAvailableCapacity(
                    requestedCapacity
            );

        } else {

            int occupiedCapacity =
                    event.getCapacity()
                            - event.getAvailableCapacity();

            if (requestedCapacity
                    < occupiedCapacity) {

                throw badRequest(
                        "La capacidad no puede ser menor "
                                + "que los cupos ya ocupados: "
                                + occupiedCapacity
                );
            }

            event.setAvailableCapacity(
                    requestedCapacity
                            - occupiedCapacity
            );
        }

        event.setTitle(title);
        event.setDescription(description);
        event.setModality(modality);
        event.setLocation(location);
        event.setVirtualUrl(virtualUrl);
        event.setCapacity(requestedCapacity);

        event.setRegistrationStartAt(
                request.getRegistrationStartAt()
        );

        event.setRegistrationEndAt(
                request.getRegistrationEndAt()
        );

        event.setStartAt(
                request.getStartAt()
        );

        event.setEndAt(
                request.getEndAt()
        );

        event.setStatus(
                request.getStatus()
        );
    }

    private void validateRequest(
            EventRequest request
    ) {
        if (request.getCapacity() == null
                || request.getCapacity() <= 0) {

            throw badRequest(
                    "La capacidad debe ser mayor que cero"
            );
        }

        if (request.getModality() == null) {

            throw badRequest(
                    "La modalidad es obligatoria"
            );
        }

        if (request.getStatus() == null) {

            throw badRequest(
                    "El estado es obligatorio"
            );
        }

        validateDates(
                request.getRegistrationStartAt(),
                request.getRegistrationEndAt(),
                request.getStartAt(),
                request.getEndAt()
        );
    }

    private void validateDates(
            OffsetDateTime registrationStartAt,
            OffsetDateTime registrationEndAt,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        if (registrationStartAt == null
                || registrationEndAt == null
                || startAt == null
                || endAt == null) {

            throw badRequest(
                    "Todas las fechas son obligatorias"
            );
        }

        if (!registrationStartAt
                .isBefore(registrationEndAt)) {

            throw badRequest(
                    "El inicio de inscripciones debe "
                            + "ser anterior al cierre"
            );
        }

        if (registrationEndAt
                .isAfter(startAt)) {

            throw badRequest(
                    "El cierre de inscripciones no puede "
                            + "ser posterior al inicio del evento"
            );
        }

        if (!startAt.isBefore(endAt)) {

            throw badRequest(
                    "El inicio del evento debe ser "
                            + "anterior a su finalización"
            );
        }
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

    private CategoryEntity findActiveCategory(
            Long categoryId
    ) {
        if (categoryId == null) {

            throw badRequest(
                    "La categoría es obligatoria"
            );
        }

        return categoryRepository
                .findByIdAndActiveTrue(categoryId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Categoría activa no encontrada"
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

    private void ensureManager(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        boolean allowed =
                hasRole(authentication, "ADMIN")
                        || hasRole(
                                authentication,
                                "ORGANIZER"
                        );

        if (!allowed) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo administradores y "
                            + "organizadores pueden "
                            + "gestionar eventos"
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
                            + "puede modificar este evento"
            );
        }
    }

    private boolean hasRole(
            Authentication authentication,
            String role
    ) {
        String roleAuthority =
                "ROLE_" + role;

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

    private EventResponse toResponse(
            EventEntity event
    ) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getModality(),
                event.getLocation(),
                event.getVirtualUrl(),
                event.getCapacity(),
                event.getAvailableCapacity(),
                event.getRegistrationStartAt(),
                event.getRegistrationEndAt(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus(),
                event.getOrganizer().getId(),
                event.getOrganizer().getFullName(),
                event.getOrganizer().getEmail(),
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getVersion(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private String normalizeRequired(
            String value,
            String message
    ) {
        String normalized =
                normalizeNullable(value);

        if (normalized == null) {
            throw badRequest(message);
        }

        return normalized;
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}