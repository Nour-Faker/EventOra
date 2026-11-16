package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dto.AuthResponse;
import tn.enicarthage.gestionsalon.dto.LoginRequest;
import tn.enicarthage.gestionsalon.dto.RegisterRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.service.AuthService;
import tn.enicarthage.gestionsalon.util.JwtUtil;

public class AuthController {

    private final AuthService authService = new AuthService();

    public AuthResponse login(LoginRequest r) {
        return reponse(authService.seConnecter(r.getEmail(), r.getMotDePasse()));
    }

    public AuthResponse register(RegisterRequest r) {
        Utilisateur u;
        if ("EXPOSANT".equals(r.getRole())) {
            u = authService.inscrireExposant(r.getNom(), r.getPrenom(), r.getEmail(),
                    r.getMotDePasse(), r.getTelephone(),
                    r.getNomEntreprise(), r.getDescription());
        } else if ("VISITEUR".equals(r.getRole())) {
            u = authService.inscrireVisiteur(r.getNom(), r.getPrenom(), r.getEmail(),
                    r.getMotDePasse(), r.getTelephone());
        } else {
            throw new IllegalArgumentException("Role invalide : VISITEUR ou EXPOSANT.");
        }
        return reponse(u);
    }

    private AuthResponse reponse(Utilisateur u) {
        String token = JwtUtil.creer(u.getId(), u.getRole().name());
        return new AuthResponse(token, new UtilisateurResponse(u));
    }
}