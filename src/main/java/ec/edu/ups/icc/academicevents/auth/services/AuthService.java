package ec.edu.ups.icc.academicevents.auth.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ec.edu.ups.icc.academicevents.auth.dtos.AuthResponse;
import ec.edu.ups.icc.academicevents.auth.dtos.LoginRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RefreshRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RegisterRequest;
import ec.edu.ups.icc.academicevents.auth.entities.RefreshTokenEntity;
import ec.edu.ups.icc.academicevents.auth.repositories.RefreshTokenRepository;
import ec.edu.ups.icc.academicevents.ratelimit.LoginAttemptService;
import ec.edu.ups.icc.academicevents.ratelimit.RateLimitExceededException;
import ec.edu.ups.icc.academicevents.ratelimit.RateLimiterService;
import ec.edu.ups.icc.academicevents.roles.entities.RoleEntity;
import ec.edu.ups.icc.academicevents.roles.entities.RoleName;
import ec.edu.ups.icc.academicevents.roles.repositories.RoleRepository;
import ec.edu.ups.icc.academicevents.security.jwt.JwtService;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final Base64.Encoder TOKEN_ENCODER =
            Base64.getUrlEncoder().withoutPadding();

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RateLimiterService rateLimiterService;

    private final LoginAttemptService loginAttemptService;

    private final long accessExpiration;

    private final long refreshExpiration;

    private final int loginLimit;

    private final long loginWindowSeconds;

    private final int registerLimit;

    private final long registerWindowSeconds;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RateLimiterService rateLimiterService,
            LoginAttemptService loginAttemptService,
            @Value("${jwt.access-expiration}")
            long accessExpiration,
            @Value("${jwt.refresh-expiration}")
            long refreshExpiration,
            @Value("${rate-limit.login.limit}")
            int loginLimit,
            @Value("${rate-limit.login.window-seconds}")
            long loginWindowSeconds,
            @Value("${rate-limit.register.limit}")
            int registerLimit,
            @Value("${rate-limit.register.window-seconds}")
            long registerWindowSeconds
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
        this.loginAttemptService = loginAttemptService;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.loginLimit = loginLimit;
        this.loginWindowSeconds = loginWindowSeconds;
        this.registerLimit = registerLimit;
        this.registerWindowSeconds = registerWindowSeconds;
    }

    @Transactional
    public void register(
            RegisterRequest request,
            String clientIp
    ) {
        enforceRateLimit(
                "rl:register:ip:" + clientIp,
                registerLimit,
                registerWindowSeconds,
                "Ha superado el límite de registros. "
                        + "Intente nuevamente más tarde."
        );

        if (userRepository.existsByEmailIgnoreCase(
                request.getEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo ya está registrado"
            );
        }

        UserEntity user = new UserEntity(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        RoleEntity participant = roleRepository
                .findByName(RoleName.PARTICIPANT)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No existe el rol PARTICIPANT"
                        )
                );

        user.addRole(participant);

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            String clientIp
    ) {
        String email = request.getEmail();

        if (loginAttemptService.isBlocked(email)) {

            throw new RateLimitExceededException(
                    "Cuenta bloqueada temporalmente por "
                            + "múltiples intentos fallidos",
                    loginAttemptService
                            .getBlockRemainingSeconds(email)
            );
        }

        enforceRateLimit(
                "rl:login:ip:" + clientIp,
                loginLimit,
                loginWindowSeconds,
                "Ha superado el límite de intentos de "
                        + "inicio de sesión. Intente "
                        + "nuevamente más tarde."
        );

        enforceRateLimit(
                "rl:login:email:" + normalize(email),
                loginLimit,
                loginWindowSeconds,
                "Ha superado el límite de intentos de "
                        + "inicio de sesión. Intente "
                        + "nuevamente más tarde."
        );

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException exception) {

            loginAttemptService
                    .registerFailedAttempt(email);

            throw invalidCredentials();
        }

        UserEntity user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);

        loginAttemptService.clearAttempts(email);

        String accessToken = jwtService.generateToken(
                user.getEmail()
        );

        IssuedRefreshToken refreshToken =
                issueRefreshToken(user, clientIp);

        return new AuthResponse(
                accessToken,
                refreshToken.rawToken(),
                accessExpiration
        );
    }

    @Transactional
    public AuthResponse refresh(
            RefreshRequest request,
            String clientIp
    ) {
        String currentTokenHash = hashToken(
                request.getRefreshToken()
        );

        RefreshTokenEntity currentToken =
                refreshTokenRepository
                        .findByTokenHash(currentTokenHash)
                        .orElseThrow(this::invalidRefreshToken);

        if (!currentToken.isValid()) {
            throw invalidRefreshToken();
        }

        UserEntity user = currentToken.getUser();

        /*
         * Primero se emite el nuevo refresh token.
         * Toda la operación está dentro de una transacción.
         */
        IssuedRefreshToken replacement =
                issueRefreshToken(user, clientIp);

        /*
         * El token anterior se revoca y se relaciona
         * con el token que lo sustituyó.
         */
        currentToken.revoke();

        currentToken.setReplacedByTokenId(
                replacement.tokenId()
        );

        refreshTokenRepository.save(currentToken);

        String newAccessToken = jwtService.generateToken(
                user.getEmail()
        );

        return new AuthResponse(
                newAccessToken,
                replacement.rawToken(),
                accessExpiration
        );
    }

    @Transactional
    public void logout(RefreshRequest request) {

        String tokenHash = hashToken(
                request.getRefreshToken()
        );

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(token -> {

                    if (!token.isRevoked()) {
                        token.revoke();
                        refreshTokenRepository.save(token);
                    }
                });
    }

    private void enforceRateLimit(
            String key,
            int limit,
            long windowSeconds,
            String message
    ) {
        RateLimiterService.RateLimitResult result =
                rateLimiterService.tryConsume(
                        key,
                        limit,
                        windowSeconds
                );

        if (!result.isAllowed()) {

            throw new RateLimitExceededException(
                    message,
                    result.getRetryAfterSeconds()
            );
        }
    }

    private String normalize(String email) {
        return email == null
                ? ""
                : email.trim().toLowerCase();
    }

    private IssuedRefreshToken issueRefreshToken(
            UserEntity user,
            String clientIp
    ) {
        String rawToken;
        String tokenHash;

        /*
         * Se evita una colisión extremadamente improbable.
         */
        do {
            rawToken = generateRawRefreshToken();
            tokenHash = hashToken(rawToken);
        } while (
                refreshTokenRepository
                        .existsByTokenHash(tokenHash)
        );

        UUID tokenId = UUID.randomUUID();

        RefreshTokenEntity entity =
                new RefreshTokenEntity();

        entity.setTokenId(tokenId);
        entity.setUser(user);
        entity.setTokenHash(tokenHash);

        entity.setExpiresAt(
                OffsetDateTime.now().plus(
                        Duration.ofMillis(refreshExpiration)
                )
        );

        if (clientIp != null && !clientIp.isBlank()) {
            entity.setCreatedByIp(clientIp);
        }

        refreshTokenRepository.save(entity);

        return new IssuedRefreshToken(
                rawToken,
                tokenId
        );
    }

    private String generateRawRefreshToken() {

        byte[] randomBytes = new byte[48];

        SECURE_RANDOM.nextBytes(randomBytes);

        return TOKEN_ENCODER.encodeToString(
                randomBytes
        );
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 no está disponible",
                    exception
            );
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas"
        );
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token inválido, expirado o revocado"
        );
    }

    private record IssuedRefreshToken(
            String rawToken,
            UUID tokenId
    ) {
    }
}