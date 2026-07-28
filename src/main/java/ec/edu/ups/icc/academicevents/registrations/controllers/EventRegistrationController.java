package ec.edu.ups.icc.academicevents.registrations.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.registrations.dtos.RegistrationResponse;
import ec.edu.ups.icc.academicevents.registrations.services.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/events/{eventId}/registrations")
@Tag(
        name = "Registrations",
        description = "Gestion de inscripciones de participantes en eventos"
)
public class EventRegistrationController {

    private final RegistrationService registrationService;

    public EventRegistrationController(
            RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @Operation(
            summary = "Inscribirse a un evento",
            description = "Solo un usuario con rol PARTICIPANT puede "
                    + "inscribirse. El evento debe estar PUBLISHED y "
                    + "dentro del periodo de inscripciones.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscripcion creada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Evento no publicado o fuera del periodo de inscripciones"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no tiene rol PARTICIPANT"
            ),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una inscripcion de este usuario para este evento"
            )
    })
    @PostMapping
    public ResponseEntity<RegistrationResponse> create(
            @PathVariable Long eventId,
            Authentication authentication
    ) {
        RegistrationResponse response =
                registrationService.create(eventId, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Listar inscritos de un evento",
            description = "Solo el ADMIN o el ORGANIZER propietario del "
                    + "evento pueden ver el listado completo de inscritos, "
                    + "paginado. Parametros: page, size, sort "
                    + "(ej. sort=registeredAt,desc).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado paginado de inscripciones"),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es ADMIN ni el organizador propietario"
            ),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    @GetMapping
    public ResponseEntity<Page<RegistrationResponse>> findAllByEvent(
            @PathVariable Long eventId,
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "registeredAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                registrationService.findAllByEvent(
                        eventId,
                        authentication,
                        pageable
                )
        );
    }
}