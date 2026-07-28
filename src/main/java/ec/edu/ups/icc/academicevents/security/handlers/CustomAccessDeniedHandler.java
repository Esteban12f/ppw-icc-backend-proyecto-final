package ec.edu.ups.icc.academicevents.security.handlers;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.ups.icc.academicevents.common.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Se ejecuta cuando un usuario autenticado intenta acceder a
 * un recurso para el cual no tiene autorización (rol
 * insuficiente). Se dispara desde el filtro de seguridad, por
 * lo tanto no pasa por el GlobalExceptionHandler y debe generar
 * el mismo formato de respuesta manualmente.
 */
@Component
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "No tiene permisos para acceder a este recurso",
                request.getRequestURI()
        );

        response.getWriter()
                .write(
                        objectMapper.writeValueAsString(body)
                );
    }
}