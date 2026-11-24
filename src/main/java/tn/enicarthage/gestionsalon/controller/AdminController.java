package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dao.AdminDAO.UtilisateurVue;
import tn.enicarthage.gestionsalon.dto.AgentRequest;
import tn.enicarthage.gestionsalon.dto.UtilisateurResponse;
import tn.enicarthage.gestionsalon.service.AdminService;
import tn.enicarthage.gestionsalon.service.AuthService;

import java.util.List;
import java.util.Map;

public class AdminController {
    private final AdminService service = new AdminService();
    private final AuthService authService = new AuthService();

    /** Cree un compte AGENT_ACCUEIL (reserve a l'administrateur). */
    public UtilisateurResponse creerAgent(AgentRequest r) {
        return new UtilisateurResponse(authService.creerAgent(r.nom(), r.prenom(), r.email(),
                r.motDePasse(), r.telephone(), r.matricule()));
    }

    public List<UtilisateurVue> utilisateurs() { return service.utilisateurs(); }
    public Map<String, Object> stats() { return service.stats(); }
    public void supprimer(int adminId, int cibleId) { service.supprimer(adminId, cibleId); }
    public void bloquer(int adminId, int cibleId, boolean bloque) { service.bloquer(adminId, cibleId, bloque); }
    public boolean compteActif(int id) { return service.compteActif(id); }

    /** Role actif de l'utilisateur, ou null s'il n'existe pas ou est bloque. */
    public String roleActif(int id) { return service.roleActif(id); }
}
