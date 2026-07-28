package ec.edu.ups.icc.academicevents.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extrae la IP real del cliente considerando el header
 * X-Forwarded-For, usado cuando la API corre detrás de un
 * proxy o balanceador (por ejemplo en Render).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }
}