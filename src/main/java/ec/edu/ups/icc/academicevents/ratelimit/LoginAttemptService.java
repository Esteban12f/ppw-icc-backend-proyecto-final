package ec.edu.ups.icc.academicevents.ratelimit;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Registra intentos fallidos de login por correo y bloquea
 * temporalmente la cuenta cuando se supera el límite permitido.
 *
 * Claves usadas en Redis:
 * - login-attempts:{email}  contador de intentos fallidos
 * - blocked-user:{email}    marca de bloqueo temporal
 */
@Service
public class LoginAttemptService {

    private static final String ATTEMPTS_PREFIX =
            "login-attempts:";

    private static final String BLOCKED_PREFIX =
            "blocked-user:";

    private final StringRedisTemplate redisTemplate;

    private final int maxAttempts;

    private final long attemptsWindowSeconds;

    private final long blockDurationSeconds;

    public LoginAttemptService(
            StringRedisTemplate redisTemplate,
            @Value("${login-block.max-attempts}")
            int maxAttempts,
            @Value("${login-block.attempts-window-seconds}")
            long attemptsWindowSeconds,
            @Value("${login-block.block-duration-seconds}")
            long blockDurationSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.attemptsWindowSeconds = attemptsWindowSeconds;
        this.blockDurationSeconds = blockDurationSeconds;
    }

    public boolean isBlocked(String email) {

        String key = BLOCKED_PREFIX + normalize(email);

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }

    public long getBlockRemainingSeconds(String email) {

        String key = BLOCKED_PREFIX + normalize(email);

        Long ttl = redisTemplate.getExpire(key);

        return (ttl != null && ttl > 0)
                ? ttl
                : blockDurationSeconds;
    }

    public void registerFailedAttempt(String email) {

        String attemptsKey =
                ATTEMPTS_PREFIX + normalize(email);

        Long attempts = redisTemplate
                .opsForValue()
                .increment(attemptsKey);

        if (attempts != null && attempts == 1L) {

            redisTemplate.expire(
                    attemptsKey,
                    Duration.ofSeconds(attemptsWindowSeconds)
            );
        }

        if (attempts != null
                && attempts >= maxAttempts) {

            String blockedKey =
                    BLOCKED_PREFIX + normalize(email);

            redisTemplate.opsForValue().set(
                    blockedKey,
                    "1",
                    Duration.ofSeconds(blockDurationSeconds)
            );

            redisTemplate.delete(attemptsKey);
        }
    }

    public void clearAttempts(String email) {

        redisTemplate.delete(
                ATTEMPTS_PREFIX + normalize(email)
        );
    }

    private String normalize(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase();
    }
}