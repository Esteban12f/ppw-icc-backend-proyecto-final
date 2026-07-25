package ec.edu.ups.icc.academicevents.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {


    private final SecretKey secretKey;

    private final long expiration;

    private final String issuer;


    public JwtService(
            @Value("${jwt.secret}") String secret,

            @Value("${jwt.access-expiration}")
            long expiration,

            @Value("${jwt.issuer}")
            String issuer
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expiration = expiration;

        this.issuer = issuer;
    }


    /**
     * Genera un JWT utilizando el correo del usuario.
     */
    public String generateToken(String email) {


        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + expiration
        );


        return Jwts.builder()

                .subject(email)

                .issuer(issuer)

                .issuedAt(now)

                .expiration(expiry)

                .signWith(secretKey)

                .compact();
    }


    /**
     * Extrae el correo almacenado en el subject.
     */
    public String extractUsername(String token) {

        return extractClaims(token)
                .getSubject();
    }



    /**
     * Valida firma y expiración.
     */
    public boolean validateToken(String token) {

        try {

            extractClaims(token);

            return true;

        } catch (Exception exception) {

            return false;
        }
    }



    private Claims extractClaims(String token) {


        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();

    }

}
