package ec.edu.ups.icc.academicevents.sessions.controllers;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.services.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events/{eventId}/sessions")
@Tag(
        name = "Sessions",
        description = "Gestion de sesiones de eventos academicos"
)
public class EventSessionController {

    private final SessionService sessionService;

    public EventSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(
            summary = "Listar sesiones de un evento",
            description = "Devuelve todas las sesiones del evento indicado, "
                    + "ordenadas por fecha de inicio. Endpoint publico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de sesiones"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    @GetMapping
    public ResponseEntity<Page<SessionResponse>> findAllByEvent(
            @PathVariable Long eventId,
            @PageableDefault(
                    size = 10,
                    sort = "startAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                sessionService.findAllByEvent(eventId, pageable)
        );
    }

    @Operation(
            summary = "Crear una sesion para un evento",
            description = "Solo el ADMIN o el ORGANIZER propietario del "
                    + "evento pueden crear sesiones. Las fechas deben caer "
                    + "dentro del rango del evento.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sesion creada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Fechas invalidas o fuera del rango del evento"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es ADMIN ni el organizador "
                            + "propietario del evento"
            ),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una sesion con el mismo titulo y "
                            + "fecha de inicio en ese evento"
            )
    })
    @PostMapping
    public ResponseEntity<SessionResponse> create(
            @PathVariable Long eventId,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication
    ) {
        SessionResponse response = sessionService.create(
                eventId, request, authentication
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}