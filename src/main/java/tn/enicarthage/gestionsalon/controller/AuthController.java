package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dto.AuthConfigResponse;
import tn.enicarthage.gestionsalon.dto.AuthResponse;
import tn.enicarthage.gestionsalon.dto.EmailRequest;
import tn.enicarthage.gestionsalon.dto.InscriptionResponse;
import tn.enicarthage.gestionsalon.dto.LoginRequest;
import tn.enicarthage.gestionsalon.dto.RegisterRequest;
import tn.enicarthage.gestionsalon.dto.ResetPasswordRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.dto.VerifyEmailRequest;
import tn.enicarthage.gestionsalon.exception.ServiceIndisponibleException;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.service.AuthService;
import tn.enicarthage.gestionsalon.service.EmailService;
import tn.enicarthage.gestionsalon.service.VerificationService;
import tn.enicarthage.gestionsalon.util.JwtUtil;

public class AuthController {

    private final AuthService authService = new AuthService();
    private final VerificationService verificationService = new VerificationService();

    public AuthResponse login(LoginRequest r) {
        return reponse(authService.seConnecter(r.getEmail(), r.getMotDePasse()));
    }

    public InscriptionResponse register(RegisterRequest r) {
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
        // Emails desactives (pas de RESEND_API_KEY) : compte actif tout de suite et connexion directe
        if (!EmailService.actif()) {
            AuthResponse auth = reponse(u);
            return new InscriptionResponse(false, u.getEmail(), false, auth.token(), auth.user());
        }
        // Le compte existe deja : si l'email ne part pas, l'utilisateur pourra demander un nouveau code
        boolean envoye = true;
        try {
            verificationService.envoyerCodeVerification(u);
        } catch (ServiceIndisponibleException e) {
            envoye = false;
        }
        return new InscriptionResponse(true, u.getEmail(), envoye, null, null);
    }

    public AuthConfigResponse config() {
        return new AuthConfigResponse(EmailService.actif());
    }

    // Code correct : le compte est active et l'utilisateur est connecte directement
    public AuthResponse verifierEmail(VerifyEmailRequest r) {
        return reponse(verificationService.verifierEmail(r.email(), r.code()));
    }

    public void renvoyerCode(EmailRequest r) {
        verificationService.renvoyerCodeVerification(r.email());
    }

    public void motDePasseOublie(EmailRequest r) {
        verificationService.motDePasseOublie(r.email());
    }

    public void reinitialiserMotDePasse(ResetPasswordRequest r) {
        verificationService.reinitialiserMotDePasse(r.email(), r.code(), r.nouveauMotDePasse());
    }

    public UtilisateurResponse profil(int userId) {
        return new UtilisateurResponse(authService.profil(userId));
    }

    private AuthResponse reponse(Utilisateur u) {
        String token = JwtUtil.creer(u.getId(), u.getRole().name());
        return new AuthResponse(token, new UtilisateurResponse(u));
    }
}
