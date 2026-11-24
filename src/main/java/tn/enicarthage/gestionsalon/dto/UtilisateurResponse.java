package tn.enicarthage.gestionsalon.dto;

import tn.enicarthage.gestionsalon.model.Utilisateur;

public record UtilisateurResponse(int id, String nom, String prenom, String email, String role) {

    public UtilisateurResponse(Utilisateur u) {
        this(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole().name());
    }
}