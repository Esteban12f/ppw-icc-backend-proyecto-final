package ec.edu.ups.icc.academicevents.events.controllers;

import ec.edu.ups.icc.academicevents.events.dtos.EventRequest;
import ec.edu.ups.icc.academicevents.events.dtos.EventResponse;
import ec.edu.ups.icc.academicevents.events.services.EventService;

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
public class EventController {

    private final EventService eventService;

    public EventController(
            EventService eventService
    ) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>>
    findAll() {

        return ResponseEntity.ok(
                eventService.findAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                eventService.findById(id)
        );
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody
            EventRequest request,
            Authentication authentication
    ) {
        EventResponse response =
                eventService.create(
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody
            EventRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                eventService.update(
                        id,
                        request,
                        authentication
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        eventService.delete(
                id,
                authentication
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}