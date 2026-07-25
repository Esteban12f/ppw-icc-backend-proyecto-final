package ec.edu.ups.icc.academicevents.auth.controllers;

import ec.edu.ups.icc.academicevents.auth.dtos.*;
import ec.edu.ups.icc.academicevents.auth.services.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {

        this.authService = authService;

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(

            @Valid @RequestBody RegisterRequest request

    ) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        "Usuario registrado correctamente");

    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(

            @Valid @RequestBody LoginRequest request

    ) {

        return ResponseEntity.ok(
                authService.login(request));

    }

}
