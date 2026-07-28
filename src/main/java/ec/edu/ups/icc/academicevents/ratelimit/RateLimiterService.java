package ec.edu.ups.icc.academicevents.ratelimit;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Rate limiting distribuido basado en Redis usando un contador
 * de ventana fija: INCR atómico en la clave y, solo en la
 * primera petición de la ventana, se define su TTL.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResult tryConsume(
            String key,
            int limit,
            long windowSeconds
    ) {
        Long count = redisTemplate
                .opsForValue()
                .increment(key);

        if (count == null) {
            // Si Redis no responde, no se bloquea al usuario.
            return new RateLimitResult(true, 0);
        }

        if (count == 1L) {
            redisTemplate.expire(
                    key,
                    Duration.ofSeconds(windowSeconds)
            );
        }

        if (count > limit) {

            Long ttl = redisTemplate.getExpire(key);

            long retryAfter =
                    (ttl != null && ttl > 0)
                            ? ttl
                            : windowSeconds;

            return new RateLimitResult(false, retryAfter);
        }

        return new RateLimitResult(true, 0);
    }

    public static class RateLimitResult {

        private final boolean allowed;

        private final long retryAfterSeconds;

        public RateLimitResult(
                boolean allowed,
                long retryAfterSeconds
        ) {
            this.allowed = allowed;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}