package ec.edu.ups.icc.academicevents.auth.repositories;

import ec.edu.ups.icc.academicevents.auth.entities.RefreshTokenEntity;
import ec.edu.ups.icc.academicevents.users.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenEntity token
               set token.revokedAt = :revokedAt
             where token.user = :user
               and token.revokedAt is null
            """)
    int revokeAllActiveTokensByUser(
            @Param("user") UserEntity user,
            @Param("revokedAt") OffsetDateTime revokedAt
    );

    void deleteByExpiresAtBefore(OffsetDateTime dateTime);
}