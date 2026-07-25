package ec.edu.ups.icc.academicevents.auth.dtos;

public class AuthResponse {

    private final String accessToken;

    private final String refreshToken;

    private final String tokenType;

    private final Long expiresIn;

    public AuthResponse(
            String accessToken,
            String refreshToken,
            Long expiresIn
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}