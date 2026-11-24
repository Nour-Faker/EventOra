package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.dto.ChangePasswordRequest;
import tn.enicarthage.gestionsalon.dto.UpdateProfilRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.util.PasswordUtil;

/**
 * Profil de l'utilisateur connecte : consultation, modification du nom et du prenom,
 * changement de mot de passe. Le mot de passe est hache avec PasswordUtil (PBKDF2),
 * comme a l'inscription et a la connexion.
 */
public class ProfilService {

    private static final int MIN_PASSWORD = 8;
    private final UtilisateurDAO dao;

    public ProfilService(UtilisateurDAO dao) { this.dao = dao; }

    public UtilisateurResponse getProfil(int userId) {
        return new UtilisateurResponse(load(userId));
    }

    public UtilisateurResponse updateProfil(int userId, UpdateProfilRequest req) {
        if (isBlank(req.nom()) || isBlank(req.prenom()))
            throw new IllegalArgumentException("Nom et prénom obligatoires");
        load(userId);
        dao.updateIdentite(userId, req.nom().trim(), req.prenom().trim());
        return getProfil(userId);
    }

    public void changePassword(int userId, ChangePasswordRequest req) {
        if (isBlank(req.ancienMotDePasse()) || isBlank(req.nouveauMotDePasse()))
            throw new IllegalArgumentException("Champs obligatoires");
        if (req.nouveauMotDePasse().length() < MIN_PASSWORD)
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins " + MIN_PASSWORD + " caractères");

        Utilisateur u = load(userId);
        // 400 et non 401 : un 401 ferait déconnecter le front, la session est valide ici
        if (!PasswordUtil.verify(req.ancienMotDePasse(), u.getMotDePasseHash()))
            throw new IllegalArgumentException("Ancien mot de passe incorrect");

        dao.updateMotDePasse(userId, PasswordUtil.hash(req.nouveauMotDePasse()));
    }

    private Utilisateur load(int id) {
        Utilisateur u = dao.findById(id);
        if (u == null) throw new ResourceNotFoundException("Utilisateur introuvable");
        return u;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}