package ec.edu.ups.icc.academicevents.registrations.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.registrations.dtos.RegistrationResponse;
import ec.edu.ups.icc.academicevents.registrations.dtos.RegistrationStatusRequest;
import ec.edu.ups.icc.academicevents.registrations.services.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/registrations")
@Tag(
        name = "Registrations",
        description = "Gestion de inscripciones de participantes en eventos"
)
@SecurityRequirement(name = "bearerAuth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(
            RegistrationService registrationService
    ) {
        this.registrationService = registrationService;
    }

    @Operation(
            summary = "Ver mis inscripciones",
            description = "Devuelve las inscripciones del usuario autenticado, "
                    + "paginadas. Parametros: page, size, sort "
                    + "(ej. sort=registeredAt,desc)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado paginado de inscripciones del usuario"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<Page<RegistrationResponse>> findMine(
            Authentication authentication,
            @PageableDefault(
                    size = 10,
                    sort = "registeredAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                registrationService.findMine(authentication, pageable)
        );
    }

    @Operation(
            summary = "Obtener una inscripcion por id",
            description = "Solo el ADMIN, el organizador propietario del "
                    + "evento o el participante propietario pueden verla."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inscripcion encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para verla"),
            @ApiResponse(responseCode = "404", description = "La inscripcion no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RegistrationResponse> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                registrationService.findById(id, authentication)
        );
    }

    @Operation(
            summary = "Cancelar una inscripcion",
            description = "Solo el participante propietario o un ADMIN "
                    + "pueden cancelarla. Libera el cupo si estaba CONFIRMED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscripcion cancelada"),
            @ApiResponse(responseCode = "400", description = "La inscripcion ya estaba cancelada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para cancelarla"),
            @ApiResponse(responseCode = "404", description = "La inscripcion no existe")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        registrationService.cancel(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Cambiar el estado de una inscripcion",
            description = "Solo el ADMIN o el ORGANIZER propietario del "
                    + "evento pueden confirmar o rechazar. Confirmar "
                    + "descuenta un cupo; rechazar una previamente "
                    + "confirmada lo libera."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "400", description = "Transicion de estado invalida"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para gestionarla"),
            @ApiResponse(responseCode = "404", description = "La inscripcion no existe"),
            @ApiResponse(responseCode = "409", description = "No hay cupos disponibles")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<RegistrationResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                registrationService.changeStatus(
                        id,
                        request,
                        authentication
                )
        );
    }
}