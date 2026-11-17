package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.AdminDAO;
import tn.enicarthage.gestionsalon.dao.AdminDAO.UtilisateurVue;

import java.util.List;
import java.util.Map;

public class AdminService {

    private final AdminDAO dao = new AdminDAO();

    public List<UtilisateurVue> utilisateurs() { return dao.utilisateurs(); }
    public Map<String, Integer> stats() { return dao.stats(); }

    public void bloquer(int adminId, int cibleId, boolean bloque) {
        dao.bloquer(adminId, cibleId, bloque);
    }

    public boolean compteActif(int id) { return dao.compteActif(id); }

    /** Role si le compte existe et n'est pas bloque, sinon null. */
    public String roleActif(int id) { return dao.roleActif(id); }
}