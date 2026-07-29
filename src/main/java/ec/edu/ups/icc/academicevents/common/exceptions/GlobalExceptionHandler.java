package ec.edu.ups.icc.academicevents.common.exceptions;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import ec.edu.ups.icc.academicevents.common.dtos.ErrorResponse;
import ec.edu.ups.icc.academicevents.ratelimit.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejo centralizado de excepciones para toda la API.
 *
 * Todas las respuestas de error siguen el mismo formato:
 * fecha, código HTTP, código interno, mensaje y ruta.
 *
 * No se exponen detalles internos (stack traces, nombres de
 * clases, mensajes de la base de datos) en las respuestas HTTP.
 * Esos detalles solo se registran en el log del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura los límites de solicitudes verificados
     * directamente en un servicio (login, registro).
     * Incluye el header Retry-After con los segundos
     * que faltan para poder reintentar.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                exception.getReason() != null
                        ? exception.getReason()
                        : "Demasiadas solicitudes",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        "Retry-After",
                        String.valueOf(
                                exception.getRetryAfterSeconds()
                        )
                )
                .body(body);
    }

    /**
     * Captura las excepciones lanzadas manualmente en los
     * servicios (recurso no encontrado, duplicados, reglas
     * de negocio, acceso prohibido, sin cupos, etc.).
     *
     * Este es el tipo de excepción usado en Events, Sessions,
     * Registrations y Auth.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();

        ErrorResponse body = new ErrorResponse(
                statusCode.value(),
                codeFor(statusCode),
                exception.getReason() != null
                        ? exception.getReason()
                        : "Error en la solicitud",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(statusCode)
                .body(body);
    }

    /**
     * Captura errores de validación de campos anotados con
     * @Valid en los DTO de entrada (@NotNull, @NotBlank,
     * @Email, @Size, etc.).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::toFieldErrorDetail)
                        .toList();

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Uno o más campos no son válidos",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    /**
     * Captura JSON malformado o con un tipo de dato incorrecto
     * en el body de la solicitud.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST_BODY",
                "El cuerpo de la solicitud no es un JSON válido",
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    /**
     * Captura parámetros de consulta obligatorios que faltan.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "MISSING_PARAMETER",
                "Falta el parámetro obligatorio: "
                        + exception.getParameterName(),
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    /**
     * Captura parámetros con un tipo de dato incorrecto, por
     * ejemplo /events/abc cuando se espera un id numérico.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER_TYPE",
                "El parámetro '" + exception.getName()
                        + "' tiene un formato inválido",
                request.getRequestURI()
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    /**
     * Captura violaciones de restricciones de la base de datos
     * (duplicados, llaves foráneas) que no fueron validadas
     * antes en el servicio.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        logger.warn(
                "Violación de integridad de datos en {}: {}",
                request.getRequestURI(),
                exception.getMessage()
        );

        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "DATA_INTEGRITY_VIOLATION",
                "La operación viola una restricción de datos "
                        + "(posible duplicado o referencia inválida)",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    /**
     * Captura solicitudes a un método HTTP no soportado por
     * el endpoint (por ejemplo, un PATCH donde solo existe GET).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHOD_NOT_ALLOWED",
                "El método HTTP no está soportado en este endpoint",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body);
    }

    /**
     * Último recurso: cualquier excepción no controlada
     * explícitamente. Nunca se expone el mensaje real de la
     * excepción al cliente, solo se registra en el log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Error no controlado en {}",
                request.getRequestURI(),
                exception
        );

        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado. Intente nuevamente.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(
            FieldError fieldError
    ) {
        return new ErrorResponse.FieldErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    private String codeFor(HttpStatusCode statusCode) {

        if (statusCode == HttpStatus.BAD_REQUEST) {
            return "BAD_REQUEST";
        }

        if (statusCode == HttpStatus.UNAUTHORIZED) {
            return "UNAUTHORIZED";
        }

        if (statusCode == HttpStatus.FORBIDDEN) {
            return "FORBIDDEN";
        }

        if (statusCode == HttpStatus.NOT_FOUND) {
            return "NOT_FOUND";
        }

        if (statusCode == HttpStatus.CONFLICT) {
            return "CONFLICT";
        }

        if (statusCode == HttpStatus.TOO_MANY_REQUESTS) {
            return "TOO_MANY_REQUESTS";
        }

        return "ERROR";
    }
}