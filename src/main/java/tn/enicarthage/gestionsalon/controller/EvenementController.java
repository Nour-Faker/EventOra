package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dto.EvenementRequest;
import tn.enicarthage.gestionsalon.model.Evenement;
import tn.enicarthage.gestionsalon.service.EvenementService;

import java.util.List;

public class EvenementController {

    private final EvenementService service = new EvenementService();

    public List<Evenement> lister() {
        return service.getAllEvenements();
    }

    public Evenement obtenir(int id) {
        return service.getEvenementById(id);
    }

    public Evenement creer(EvenementRequest r) {
        Evenement e = new Evenement();
        remplir(e, r);
        service.createEvenement(e);
        return e;
    }

    public Evenement modifier(int id, EvenementRequest r) {
        Evenement e = service.getEvenementById(id);
        remplir(e, r);
        service.updateEvenement(e);
        return e;
    }

    public void supprimer(int id) {
        service.deleteEvenement(id);
    }

    private void remplir(Evenement e, EvenementRequest r) {
        e.setTitre(r.titre());
        e.setDescription(r.description());
        e.setDateDebut(r.dateDebut());
        e.setDateFin(r.dateFin());
        e.setLieu(r.lieu());
    }
}