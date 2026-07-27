package ec.edu.ups.icc.academicevents.sessions.controllers;

import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.services.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@Tag(
        name = "Sessions",
        description = "Gestion de sesiones de eventos academicos"
)
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(
            summary = "Listar sesiones proximas",
            description = "Devuelve todas las sesiones cuya fecha de inicio "
                    + "aun no ha ocurrido, sin importar el evento al que "
                    + "pertenezcan. Endpoint publico."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de sesiones proximas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/upcoming")
    public ResponseEntity<List<SessionResponse>> findUpcoming() {
        return ResponseEntity.ok(sessionService.findUpcoming());
    }

    @Operation(
            summary = "Obtener una sesion por id",
            description = "Endpoint publico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion encontrada"),
            @ApiResponse(responseCode = "404", description = "La sesion no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.findById(id));
    }

    @Operation(
            summary = "Actualizar una sesion",
            description = "Solo el ADMIN o el ORGANIZER propietario del "
                    + "evento pueden actualizar la sesion. Las nuevas "
                    + "fechas deben caer dentro del rango del evento.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion actualizada"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Fechas invalidas o fuera del rango del evento",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = "{\"message\":\"La sesion debe estar "
                                            + "dentro del periodo del evento\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es ADMIN ni el organizador "
                            + "propietario del evento"
            ),
            @ApiResponse(responseCode = "404", description = "La sesion no existe"),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una sesion con el mismo titulo y "
                            + "fecha de inicio en ese evento"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(sessionService.update(id, request, authentication));
    }

    @Operation(
            summary = "Eliminar una sesion",
            description = "Borrado fisico. Solo el ADMIN o el ORGANIZER "
                    + "propietario del evento pueden eliminarla.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion eliminada"),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es ADMIN ni el organizador "
                            + "propietario del evento"
            ),
            @ApiResponse(responseCode = "404", description = "La sesion no existe")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        sessionService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}