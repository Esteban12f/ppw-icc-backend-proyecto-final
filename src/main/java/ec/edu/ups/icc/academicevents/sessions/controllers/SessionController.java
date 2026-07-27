package ec.edu.ups.icc.academicevents.sessions.controllers;

import ec.edu.ups.icc.academicevents.sessions.dtos.SessionRequest;
import ec.edu.ups.icc.academicevents.sessions.dtos.SessionResponse;
import ec.edu.ups.icc.academicevents.sessions.services.SessionService;

import jakarta.validation.Valid;

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
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<SessionResponse>> findUpcoming() {
        return ResponseEntity.ok(sessionService.findUpcoming());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(sessionService.update(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        sessionService.delete(id, authentication);
        return ResponseEntity.noContent().build();
    }
}