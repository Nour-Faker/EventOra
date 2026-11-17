package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dao.StandVueDAO.DemandeVue;
import tn.enicarthage.gestionsalon.dao.StandVueDAO.StandVue;
import tn.enicarthage.gestionsalon.service.StandService;

import java.math.BigDecimal;
import java.util.List;

public class StandController {
    private final StandService service = new StandService();

    public List<StandVue> stands(int evenementId) { return service.stands(evenementId); }
    public List<DemandeVue> mesDemandes(int exposantId) { return service.mesDemandes(exposantId); }
    public List<DemandeVue> toutesLesDemandes() { return service.toutesLesDemandes(); }
    public StandVue creerStand(int evenementId, int categorieId, String numero,
                               String emplacement, BigDecimal superficie, BigDecimal prix) {
        return service.creerStand(evenementId, categorieId, numero, emplacement, superficie, prix);
    }
    public StandVue modifierStand(int standId, int categorieId, String numero,
                                  String emplacement, BigDecimal superficie, BigDecimal prix) {
        return service.modifierStand(standId, categorieId, numero, emplacement, superficie, prix);
    }
    public void supprimerStand(int standId) { service.supprimerStand(standId); }
    public DemandeVue demander(int exposantId, int standId) { return service.demander(exposantId, standId); }
    public DemandeVue traiter(int adminId, int demandeId, String decision, String motif) {
        return service.traiter(adminId, demandeId, decision, motif);
    }
}
