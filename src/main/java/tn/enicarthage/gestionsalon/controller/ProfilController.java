package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.dto.ChangePasswordRequest;
import tn.enicarthage.gestionsalon.dto.UpdateProfilRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.service.ProfilService;

/**
 * GET /me, PUT /me, PUT /me/password : tout utilisateur connecte (BF-03, BF-04).
 * L'identifiant vient TOUJOURS du jeton : impossible de lire ou modifier le profil d'un autre.
 */
public class ProfilController {

    private final ProfilService service = new ProfilService(new UtilisateurDAO());

    public UtilisateurResponse profil(int userId) { return service.getProfil(userId); }

    public UtilisateurResponse modifier(int userId, UpdateProfilRequest r) {
        return service.updateProfil(userId, r);
    }

    public void changerMotDePasse(int userId, ChangePasswordRequest r) {
        service.changePassword(userId, r);
    }
}
