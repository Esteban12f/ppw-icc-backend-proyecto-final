package ec.edu.ups.icc.academicevents.security.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Academic Events API",
                version = "v1",
                description = "API REST para la gestion de eventos "
                        + "academicos, sesiones e inscripciones"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Enviar el access token JWT obtenido en /auth/login, "
                + "sin el prefijo Bearer (Swagger lo agrega automaticamente)"
)
public class OpenApiConfig {
}