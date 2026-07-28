package ec.edu.ups.icc.academicevents.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.auth.dtos.AuthResponse;
import ec.edu.ups.icc.academicevents.auth.dtos.LoginRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RefreshRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RegisterRequest;
import ec.edu.ups.icc.academicevents.auth.services.AuthService;
import ec.edu.ups.icc.academicevents.ratelimit.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody
            RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.register(
                request,
                ClientIpResolver.resolve(httpRequest)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Usuario registrado correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                authService.login(
                        request,
                        ClientIpResolver.resolve(httpRequest)
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody
            RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                authService.refresh(
                        request,
                        ClientIpResolver.resolve(httpRequest)
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody
            RefreshRequest request
    ) {
        authService.logout(request);

        return ResponseEntity.noContent().build();
    }
}