package ec.edu.ups.icc.academicevents.security.handlers;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ec.edu.ups.icc.academicevents.common.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Se ejecuta cuando una solicitud llega a un endpoint protegido
 * sin un token válido (ausente, expirado o mal formado). Se
 * dispara desde el filtro de seguridad, por lo tanto no pasa
 * por el GlobalExceptionHandler y debe generar el mismo formato
 * de respuesta manualmente.
 *
 * Se usa un ObjectMapper propio (no inyectado) porque este
 * handler corre fuera del ciclo normal de Spring MVC.
 */
@Component
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Token de autenticación ausente, inválido o expirado",
                request.getRequestURI()
        );

        response.getWriter()
                .write(
                        OBJECT_MAPPER.writeValueAsString(body)
                );
    }
}