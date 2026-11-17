package tn.enicarthage.gestionsalon.dto;

import tn.enicarthage.gestionsalon.model.Utilisateur;

public class UtilisateurResponse {
    private final int id;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String role;

    public UtilisateurResponse(Utilisateur u) {
        this.id = u.getId();
        this.nom = u.getNom();
        this.prenom = u.getPrenom();
        this.email = u.getEmail();
        this.role = u.getRole().name();
    }
}