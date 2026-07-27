package ec.edu.ups.icc.academicevents.sessions.controllers;

import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.services.SessionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events/{eventId}/sessions")
public class EventSessionController {

    private final SessionService sessionService;

    public EventSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> findAllByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(sessionService.findAllByEvent(eventId));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(
            @PathVariable Long eventId,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication
    ) {
        SessionResponse response = sessionService.create(eventId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}