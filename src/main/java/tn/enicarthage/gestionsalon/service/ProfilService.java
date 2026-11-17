package tn.enicarthage.gestionsalon.service;

import org.mindrot.jbcrypt.BCrypt;                       // ADAPTEZ : même lib de hash que AuthService
import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.dto.ChangePasswordRequest;
import tn.enicarthage.gestionsalon.dto.UpdateProfilRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Utilisateur;

/**
 * ADAPTEZ aux méthodes réelles de UtilisateurDAO :
 *   Utilisateur findById(int id)                       (null si absent)
 *   void updateIdentite(int id, String nom, String prenom)
 *   void updateMotDePasse(int id, String hash)
 * et à UtilisateurResponse (ici : UtilisateurResponse.from(Utilisateur)).
 */
public class ProfilService {

    private static final int MIN_PASSWORD = 8;
    private final UtilisateurDAO dao;

    public ProfilService(UtilisateurDAO dao) { this.dao = dao; }

    public UtilisateurResponse getProfil(int userId) {
        return UtilisateurResponse.from(load(userId));
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
        if (!BCrypt.checkpw(req.ancienMotDePasse(), u.getMotDePasseHash()))   // ADAPTEZ : getter
            throw new IllegalArgumentException("Ancien mot de passe incorrect");

        dao.updateMotDePasse(userId, BCrypt.hashpw(req.nouveauMotDePasse(), BCrypt.gensalt(12)));
    }

    private Utilisateur load(int id) {
        Utilisateur u = dao.findById(id);
        if (u == null) throw new ResourceNotFoundException("Utilisateur introuvable");
        return u;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}