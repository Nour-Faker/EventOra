package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.AdminDAO;
import tn.enicarthage.gestionsalon.dao.AdminDAO.UtilisateurVue;
import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Map;

public class AdminService {

    private final AdminDAO dao = new AdminDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public List<UtilisateurVue> utilisateurs() { return dao.utilisateurs(); }
    public Map<String, Object> stats() { return dao.stats(); }

    // Bloque ou debloque un compte (reserve a l'administrateur par le controleur)
    public void bloquer(int adminId, int cibleId, boolean bloque) {
        if (adminId == cibleId) {
            throw new IllegalArgumentException(
                    "Un administrateur ne peut pas bloquer son propre compte.");
        }
        if (utilisateurDAO.findRole(cibleId) == null) {
            throw new ResourceNotFoundException("Utilisateur introuvable : " + cibleId);
        }
        utilisateurDAO.setBloque(cibleId, bloque);
    }

    // BF-07 : supprimer un utilisateur. Refuse (409) s'il a des billets ou un stand reserve :
    // dans ce cas l'administrateur bloque le compte (est_bloque) au lieu de le supprimer.
    public void supprimer(int adminId, int cibleId) {
        if (adminId == cibleId) {
            throw new IllegalArgumentException(
                    "Un administrateur ne peut pas supprimer son propre compte.");
        }
        if (utilisateurDAO.findRole(cibleId) == null) {
            throw new ResourceNotFoundException("Utilisateur introuvable : " + cibleId);
        }
        if (dao.compterBillets(cibleId) > 0) {
            throw new ConflictException(
                    "Cet utilisateur possede des billets : bloquez son compte au lieu de le supprimer.");
        }
        if (dao.compterStandsReserves(cibleId) > 0) {
            throw new ConflictException(
                    "Cet exposant a un stand reserve : bloquez son compte au lieu de le supprimer.");
        }
        utilisateurDAO.delete(cibleId);
    }

    public boolean compteActif(int id) { return dao.compteActif(id); }

    /** Role si le compte existe et n'est pas bloque, sinon null. */
    public String roleActif(int id) { return dao.roleActif(id); }
}