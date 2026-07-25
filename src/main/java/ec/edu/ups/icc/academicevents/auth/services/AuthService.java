package ec.edu.ups.icc.academicevents.auth.services;

import ec.edu.ups.icc.academicevents.auth.dtos.AuthResponse;
import ec.edu.ups.icc.academicevents.auth.dtos.LoginRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RefreshRequest;
import ec.edu.ups.icc.academicevents.auth.dtos.RegisterRequest;
import ec.edu.ups.icc.academicevents.auth.entities.RefreshTokenEntity;
import ec.edu.ups.icc.academicevents.auth.repositories.RefreshTokenRepository;
import ec.edu.ups.icc.academicevents.roles.entities.RoleEntity;
import ec.edu.ups.icc.academicevents.roles.entities.RoleName;
import ec.edu.ups.icc.academicevents.roles.repositories.RoleRepository;
import ec.edu.ups.icc.academicevents.security.jwt.JwtService;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import ec.edu.ups.icc.academicevents.users.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

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

    private final long accessExpiration;

    private final long refreshExpiration;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${jwt.access-expiration}")
            long accessExpiration,
            @Value("${jwt.refresh-expiration}")
            long refreshExpiration
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    @Transactional
    public void register(RegisterRequest request) {

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
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserEntity user = userRepository
                .findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(this::invalidCredentials);

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