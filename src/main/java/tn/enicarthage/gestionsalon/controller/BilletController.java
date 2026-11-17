package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dao.BilletDAO.BilletVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.ScanVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.TypeBilletVue;
import tn.enicarthage.gestionsalon.service.BilletService;

import java.math.BigDecimal;
import java.util.List;

public class BilletController {

    private final BilletService service = new BilletService();

    public List<TypeBilletVue> types(int evenementId) { return service.types(evenementId); }
    public TypeBilletVue creerType(int evenementId, String nom, BigDecimal prix, int quota) {
        return service.creerType(evenementId, nom, prix, quota);
    }
    public TypeBilletVue modifierType(int typeId, String nom, BigDecimal prix, int quota) {
        return service.modifierType(typeId, nom, prix, quota);
    }
    public void supprimerType(int typeId) { service.supprimerType(typeId); }
    public List<BilletVue> mesBillets(int visiteurId) { return service.mesBillets(visiteurId); }
    public BilletVue acheter(int visiteurId, int typeBilletId) { return service.acheter(visiteurId, typeBilletId); }
    public void annuler(int visiteurId, int billetId) { service.annuler(visiteurId, billetId); }
    public ScanVue scanner(int agentId, String code) { return service.scanner(agentId, code); }
}
