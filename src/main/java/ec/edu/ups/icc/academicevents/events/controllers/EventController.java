package ec.edu.ups.icc.academicevents.events.controllers;

import ec.edu.ups.icc.academicevents.events.dtos.EventRequest;
import ec.edu.ups.icc.academicevents.events.dtos.EventResponse;
import ec.edu.ups.icc.academicevents.events.services.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Consulta y gestión de eventos académicos")
public class EventController {

        private final EventService eventService;

        public EventController(
                        EventService eventService) {
                this.eventService = eventService;
        }

        @Operation(summary = "Listar eventos académicos", description = "Devuelve todos los eventos que no han sido eliminados lógicamente.")
        @GetMapping
        public ResponseEntity<List<EventResponse>> findAll() {

                return ResponseEntity.ok(
                                eventService.findAll());
        }

        @Operation(summary = "Obtener un evento por ID", description = "Devuelve la información detallada de un evento académico no eliminado.")
        @GetMapping("/{id}")
        public ResponseEntity<EventResponse> findById(
                        @PathVariable Long id) {
                return ResponseEntity.ok(
                                eventService.findById(id));
        }

        @Operation(summary = "Crear un evento académico", description = "Crea un nuevo evento asociado al organizador autenticado. Requiere rol ADMIN u ORGANIZER.")
        @PostMapping
        public ResponseEntity<EventResponse> create(
                        @Valid @RequestBody EventRequest request,
                        Authentication authentication) {
                EventResponse response = eventService.create(
                                request,
                                authentication);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @Operation(summary = "Actualizar un evento académico", description = "Actualiza un evento existente. Un organizador solo puede modificar eventos de su propiedad.")
        @PutMapping("/{id}")
        public ResponseEntity<EventResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody EventRequest request,
                        Authentication authentication) {
                return ResponseEntity.ok(
                                eventService.update(
                                                id,
                                                request,
                                                authentication));
        }

        @Operation(summary = "Eliminar un evento académico", description = "Realiza la eliminación lógica del evento mediante el campo deleted.")
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id,
                        Authentication authentication) {
                eventService.delete(
                                id,
                                authentication);

                return ResponseEntity
                                .noContent()
                                .build();
        }
}