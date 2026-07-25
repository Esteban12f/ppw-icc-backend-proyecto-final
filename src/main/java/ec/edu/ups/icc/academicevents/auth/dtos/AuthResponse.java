package ec.edu.ups.icc.academicevents.auth.dtos;



public class AuthResponse {


    private String accessToken;

    private String tokenType;

    private Long expiresIn;



    public AuthResponse(
            String accessToken,
            Long expiresIn
    ){

        this.accessToken=accessToken;

        this.tokenType="Bearer";

        this.expiresIn=expiresIn;
    }


    public String getAccessToken(){
        return accessToken;
    }


    public String getTokenType(){
        return tokenType;
    }


    public Long getExpiresIn(){
        return expiresIn;
    }

}