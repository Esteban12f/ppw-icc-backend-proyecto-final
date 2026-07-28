package ec.edu.ups.icc.academicevents.ratelimit;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Se lanza cuando se supera un límite de solicitudes verificado
 * directamente en un servicio (login, registro). Lleva los
 * segundos que faltan para poder reintentar, usados para
 * construir el header Retry-After.
 */
public class RateLimitExceededException
        extends ResponseStatusException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(
            String message,
            long retryAfterSeconds
    ) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}