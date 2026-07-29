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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Authentication authentication;

    private SessionService sessionService;

    private UserEntity organizer;

    private UserEntity otherOrganizer;

    private EventEntity event;

    @BeforeEach
    void setUp() {

        sessionService = new SessionService(
                sessionRepository,
                eventRepository,
                userRepository,
                entityManager
        );

        organizer = new UserEntity();

        ReflectionTestUtils.setField(
                organizer,
                "id",
                10L
        );

        organizer.setFirstName("Organizador");
        organizer.setLastName("Principal");
        organizer.setEmail(
                "organizer@academic.test"
        );

        otherOrganizer = new UserEntity();

        ReflectionTestUtils.setField(
                otherOrganizer,
                "id",
                99L
        );

        otherOrganizer.setFirstName("Otro");
        otherOrganizer.setLastName("Organizador");
        otherOrganizer.setEmail(
                "otro.organizer@academic.test"
        );

        event = new EventEntity();

        ReflectionTestUtils.setField(
                event,
                "id",
                1L
        );

        event.setTitle("Evento de prueba");
        event.setOrganizer(organizer);
        event.setStatus(EventStatus.DRAFT);
        event.setDeleted(false);

        event.setStartAt(
                OffsetDateTime.parse(
                        "2026-08-01T08:00:00-05:00"
                )
        );

        event.setEndAt(
                OffsetDateTime.parse(
                        "2026-08-01T18:00:00-05:00"
                )
        );

        lenient()
                .when(authentication.getName())
                .thenReturn(
                        "organizer@academic.test"
                );

        lenient()
                .when(
                        authentication.isAuthenticated()
                )
                .thenReturn(true);
    }

    @Test
    void create_whenValidRequestAndOwnerOrganizer_savesSession() {

        mockAsOrganizerOwner();

        when(
                eventRepository
                        .findByIdAndDeletedFalse(1L)
        ).thenReturn(
                Optional.of(event)
        );

        when(
                sessionRepository.saveAndFlush(
                        any(SessionEntity.class)
                )
        ).thenAnswer(invocation -> {

            SessionEntity savedSession =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    savedSession,
                    "id",
                    100L
            );

            return savedSession;
        });

        SessionResponse response =
                sessionService.create(
                        1L,
                        validRequest(),
                        authentication
                );

        assertThat(response).isNotNull();

        assertThat(response.getTitle())
                .isEqualTo(
                        "Charla de arquitectura"
                );

        verify(sessionRepository)
                .saveAndFlush(
                        any(SessionEntity.class)
                );
    }

    @Test
    void create_whenActorIsParticipant_throwsForbidden() {

        when(authentication.getAuthorities())
                .thenAnswer(invocation ->
                        List.of(
                                new SimpleGrantedAuthority(
                                        "PARTICIPANT"
                                )
                        )
                );

        assertThatThrownBy(() ->
                sessionService.create(
                        1L,
                        validRequest(),
                        authentication
                )
        )
                .isInstanceOf(
                        ResponseStatusException.class
                )
                .hasMessageContaining("403");

        verify(
                sessionRepository,
                never()
        ).saveAndFlush(
                any(SessionEntity.class)
        );
    }

    @Test
    void create_whenOrganizerNotOwner_throwsForbidden() {

        when(authentication.getAuthorities())
                .thenAnswer(invocation ->
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ORGANIZER"
                                )
                        )
                );

        when(
                userRepository
                        .findByEmailIgnoreCase(
                                "organizer@academic.test"
                        )
        ).thenReturn(
                Optional.of(otherOrganizer)
        );

        when(
                eventRepository
                        .findByIdAndDeletedFalse(1L)
        ).thenReturn(
                Optional.of(event)
        );

        assertThatThrownBy(() ->
                sessionService.create(
                        1L,
                        validRequest(),
                        authentication
                )
        )
                .isInstanceOf(
                        ResponseStatusException.class
                )
                .hasMessageContaining("403");

        verify(
                sessionRepository,
                never()
        ).saveAndFlush(
                any(SessionEntity.class)
        );
    }

    /**
     * Simula que el usuario autenticado tiene rol ORGANIZER
     * y es el propietario del evento.
     */
    private void mockAsOrganizerOwner() {

        when(authentication.getAuthorities())
                .thenAnswer(invocation ->
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ORGANIZER"
                                )
                        )
                );

        when(
                userRepository
                        .findByEmailIgnoreCase(
                                "organizer@academic.test"
                        )
        ).thenReturn(
                Optional.of(organizer)
        );
    }

    /**
     * Construye una solicitud válida cuya fecha se encuentra
     * dentro del horario del evento.
     */
    private SessionRequest validRequest() {

        SessionRequest request =
                new SessionRequest();

        request.setTitle(
                "Charla de arquitectura"
        );

        request.setDescription(
                "Descripción de la sesión de prueba"
        );

        request.setStartAt(
                OffsetDateTime.parse(
                        "2026-08-01T09:00:00-05:00"
                )
        );

        request.setEndAt(
                OffsetDateTime.parse(
                        "2026-08-01T11:00:00-05:00"
                )
        );

        request.setLocation(
                "Auditorio principal"
        );

        request.setVirtualUrl(null);

        return request;
    }
}