package ec.edu.ups.icc.academicevents.ratelimit;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ec.edu.ups.icc.academicevents.common.dtos.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica rate limiting general: por IP en endpoints públicos, o
 * por usuario autenticado en endpoints protegidos. Los reportes
 * usan un límite más estricto dentro de la misma categoría de
 * "autenticado".
 *
 * Los endpoints de /auth/** tienen su propio límite verificado
 * directamente en AuthService, por eso se excluyen aquí para no
 * aplicar la restricción dos veces.
 *
 * Se usa un ObjectMapper propio (no inyectado) porque este filtro
 * corre fuera del ciclo normal de serialización de Spring MVC.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private final RateLimiterService rateLimiterService;

    private final int publicLimit;

    private final long publicWindowSeconds;

    private final int authenticatedLimit;

    private final long authenticatedWindowSeconds;

    private final int reportsLimit;

    private final long reportsWindowSeconds;

    public RateLimitFilter(
            RateLimiterService rateLimiterService,
            @Value("${rate-limit.public.limit}")
            int publicLimit,
            @Value("${rate-limit.public.window-seconds}")
            long publicWindowSeconds,
            @Value("${rate-limit.authenticated.limit}")
            int authenticatedLimit,
            @Value("${rate-limit.authenticated.window-seconds}")
            long authenticatedWindowSeconds,
            @Value("${rate-limit.reports.limit}")
            int reportsLimit,
            @Value("${rate-limit.reports.window-seconds}")
            long reportsWindowSeconds
    ) {
        this.rateLimiterService = rateLimiterService;
        this.publicLimit = publicLimit;
        this.publicWindowSeconds = publicWindowSeconds;
        this.authenticatedLimit = authenticatedLimit;
        this.authenticatedWindowSeconds = authenticatedWindowSeconds;
        this.reportsLimit = reportsLimit;
        this.reportsWindowSeconds = reportsWindowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        if (HttpMethod.OPTIONS.matches(
                request.getMethod()
        )) {
            return true;
        }

        String path = request.getRequestURI();

        return path.startsWith("/api/auth/")
                || path.startsWith("/api/actuator/")
                || path.startsWith("/api/swagger-ui")
                || path.startsWith("/api/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        boolean authenticated =
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication
                                instanceof AnonymousAuthenticationToken);

        String path = request.getRequestURI();

        String key;
        int limit;
        long windowSeconds;

        if (authenticated && path.contains("/reports")) {

            key = "rl:reports:" + authentication.getName();
            limit = reportsLimit;
            windowSeconds = reportsWindowSeconds;

        } else if (authenticated) {

            key = "rl:auth:" + authentication.getName();
            limit = authenticatedLimit;
            windowSeconds = authenticatedWindowSeconds;

        } else {

            String ip = ClientIpResolver.resolve(request);
            key = "rl:public:" + ip;
            limit = publicLimit;
            windowSeconds = publicWindowSeconds;
        }

        RateLimiterService.RateLimitResult result =
                rateLimiterService.tryConsume(
                        key,
                        limit,
                        windowSeconds
                );

        if (!result.isAllowed()) {

            writeTooManyRequests(
                    response,
                    path,
                    result.getRetryAfterSeconds()
            );

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            String path,
            long retryAfterSeconds
    ) throws IOException {

        response.setStatus(429);

        response.setHeader(
                "Retry-After",
                String.valueOf(retryAfterSeconds)
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                "Ha superado el límite de solicitudes. "
                        + "Intente nuevamente más tarde.",
                path
        );

        response.getWriter().write(
                OBJECT_MAPPER.writeValueAsString(body)
        );
    }
}