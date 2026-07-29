package ec.edu.ups.icc.academicevents.reports.services;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ec.edu.ups.icc.academicevents.events.entities.EventEntity;
import ec.edu.ups.icc.academicevents.events.repositories.EventRepository;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationEntity;
import ec.edu.ups.icc.academicevents.registrations.entities.RegistrationStatus;
import ec.edu.ups.icc.academicevents.registrations.repositories.RegistrationRepository;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

/**
 * Orquesta la generación de reportes: valida permisos, aplica
 * el filtro de fechas y delega la construcción del archivo a
 * PdfReportGenerator o ExcelReportGenerator.
 *
 * Los archivos se generan en memoria bajo demanda; nada se
 * guarda en disco ni en Redis.
 */
@Service
public class ReportService {

    private static final OffsetDateTime DEFAULT_FROM =
            OffsetDateTime.parse("1970-01-01T00:00:00Z");

    private final EventRepository eventRepository;

    private final RegistrationRepository registrationRepository;

    private final UserRepository userRepository;

    private final PdfReportGenerator pdfReportGenerator;

    private final ExcelReportGenerator excelReportGenerator;

    public ReportService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            PdfReportGenerator pdfReportGenerator,
            ExcelReportGenerator excelReportGenerator
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.pdfReportGenerator = pdfReportGenerator;
        this.excelReportGenerator = excelReportGenerator;
    }

    @Transactional(readOnly = true)
    public byte[] generateRegistrationsPdf(
            Long eventId,
            OffsetDateTime from,
            OffsetDateTime to,
            Authentication authentication
    ) {
        EventEntity event = findEventForReport(
                eventId,
                authentication
        );

        List<RegistrationEntity> registrations =
                findRegistrationsInRange(eventId, from, to);

        return pdfReportGenerator.generateRegistrationsList(
                event,
                registrations
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateRegistrationsExcel(
            Long eventId,
            OffsetDateTime from,
            OffsetDateTime to,
            Authentication authentication
    ) {
        EventEntity event = findEventForReport(
                eventId,
                authentication
        );

        List<RegistrationEntity> registrations =
                findRegistrationsInRange(eventId, from, to);

        return excelReportGenerator.generateRegistrationsList(
                event,
                registrations
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateCertificate(
            Long registrationId,
            Authentication authentication
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        RegistrationEntity registration =
                registrationRepository
                        .findById(registrationId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Inscripción no encontrada"
                                )
                        );

        boolean isOwner = registration.getParticipant()
                .getId()
                .equals(actor.getId());

        if (!isOwner) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el participante propietario "
                            + "puede descargar este comprobante"
            );
        }

        if (registration.getStatus()
                != RegistrationStatus.CONFIRMED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El comprobante solo está disponible "
                            + "para inscripciones confirmadas"
            );
        }

        return pdfReportGenerator
                .generateCertificate(registration);
    }

    private EventEntity findEventForReport(
            Long eventId,
            Authentication authentication
    ) {
        UserEntity actor =
                findAuthenticatedUser(authentication);

        EventEntity event = eventRepository
                .findByIdAndDeletedFalse(eventId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Evento no encontrado"
                        )
                );

        boolean isAdmin =
                hasRole(authentication, "ADMIN");

        boolean isOwner = event.getOrganizer()
                .getId()
                .equals(actor.getId());

        if (!isAdmin && !isOwner) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el organizador propietario del "
                            + "evento o un ADMIN pueden "
                            + "generar este reporte"
            );
        }

        return event;
    }

    private List<RegistrationEntity> findRegistrationsInRange(
            Long eventId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        OffsetDateTime effectiveFrom =
                from != null ? from : DEFAULT_FROM;

        OffsetDateTime effectiveTo =
                to != null ? to : OffsetDateTime.now();

        return registrationRepository
                .findAllByEvent_IdAndRegisteredAtBetweenOrderByRegisteredAtAsc(
                        eventId,
                        effectiveFrom,
                        effectiveTo
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
}