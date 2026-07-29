package ec.edu.ups.icc.academicevents.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.ups.icc.academicevents.auth.dtos.AuthResponse;
import ec.edu.ups.icc.academicevents.auth.dtos.CurrentUserResponse;
import ec.edu.ups.icc.academicevents.auth.dtos.LoginRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RefreshRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RegisterRequest;
import ec.edu.ups.icc.academicevents.auth.services.AuthService;
import ec.edu.ups.icc.academicevents.ratelimit.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registro, inicio de sesión, renovación de tokens y cierre de sesión")
public class AuthController {

        private final AuthService authService;

        public AuthController(
                        AuthService authService) {
                this.authService = authService;
        }

        @Operation(summary = "Registrar un nuevo usuario", description = "Crea una cuenta de usuario y asigna automáticamente el rol PARTICIPANT.")
        @PostMapping("/register")
        public ResponseEntity<String> register(
                        @Valid @RequestBody RegisterRequest request,
                        HttpServletRequest httpRequest) {
                authService.register(
                                request,
                                ClientIpResolver.resolve(httpRequest));

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body("Usuario registrado correctamente");
        }

        @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un access token JWT y un refresh token.")
        @PostMapping("/login")
        public ResponseEntity<AuthResponse> login(
                        @Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest) {
                return ResponseEntity.ok(
                                authService.login(
                                                request,
                                                ClientIpResolver.resolve(httpRequest)));
        }

        @Operation(summary = "Renovar tokens de autenticación", description = "Valida y rota el refresh token, devolviendo un nuevo access token y un nuevo refresh token.")
        @PostMapping("/refresh")
        public ResponseEntity<AuthResponse> refresh(
                        @Valid @RequestBody RefreshRequest request,
                        HttpServletRequest httpRequest) {
                return ResponseEntity.ok(
                                authService.refresh(
                                                request,
                                                ClientIpResolver.resolve(httpRequest)));
        }

        @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token enviado para impedir que vuelva a utilizarse.")
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @Valid @RequestBody RefreshRequest request) {
                authService.logout(request);

                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Consultar usuario autenticado", description = "Devuelve los datos y roles del usuario identificado por el access token JWT.")
        @GetMapping("/me")
        public ResponseEntity<CurrentUserResponse> me(
                        Authentication authentication) {
                return ResponseEntity.ok(
                                authService.me(authentication));
        }
}