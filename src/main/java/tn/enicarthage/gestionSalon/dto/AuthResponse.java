package tn.enicarthage.gestionsalon.dto;

public class AuthResponse {
    private final String token;
    private final UtilisateurResponse user;

    public AuthResponse(String token, UtilisateurResponse user) {
        this.token = token;
        this.user = user;
    }
}