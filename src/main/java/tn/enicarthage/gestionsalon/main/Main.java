package tn.enicarthage.gestionsalon.main;

import tn.enicarthage.gestionsalon.model.Billet;
import tn.enicarthage.gestionsalon.model.Evenement;
import tn.enicarthage.gestionsalon.service.BilletService;
import tn.enicarthage.gestionsalon.service.EvenementService;

import java.time.LocalDate;

public class Main {

    // Execute une action et affiche le refus si une regle metier l'interdit
    private static void essayer(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.println("Refuse : " + e.getMessage());
        }
    }

    private static Evenement creerEvenement(EvenementService service, String titre,
                                            LocalDate debut, LocalDate fin) {
        Evenement e = new Evenement();
        e.setTitre(titre);
        e.setDescription("Evenement de test");
        e.setDateDebut(debut);
        e.setDateFin(fin);
        e.setLieu("Sfax");
        service.createEvenement(e);
        return e;
    }

    public static void main(String[] args) {
        EvenementService evenementService = new EvenementService();
        BilletService service = new BilletService();

        Evenement futur = creerEvenement(evenementService, "Evenement futur test",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11));
        Evenement passe = creerEvenement(evenementService, "Evenement passe test",
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 11));
        System.out.println("Evenements de test crees : " + futur.getId()
                + " et " + passe.getId());

        // 1. Reservation valide (visiteur 5)
        Billet b = service.reserverBillet(5, futur.getId());
        System.out.println("Billet reserve : statut = " + b.getStatut()
                + ", code = " + b.getCodeQR());

        // 2. Reservations refusees
        essayer(() -> service.reserverBillet(5, futur.getId()));    // doublon
        essayer(() -> service.reserverBillet(999, futur.getId()));  // visiteur inconnu
        essayer(() -> service.reserverBillet(2, futur.getId()));    // c'est un exposant
        essayer(() -> service.reserverBillet(5, 999));              // evenement inconnu
        essayer(() -> service.reserverBillet(5, passe.getId()));    // evenement termine

        // 3. Scans refuses
        essayer(() -> service.scannerBillet(b.getCodeQR(), 2));     // pas un agent
        essayer(() -> service.scannerBillet("QR-INCONNU", 7));      // billet inconnu

        // 4. Scan valide par l'agent 7
        service.scannerBillet(b.getCodeQR(), 7);
        Billet scanne = service.getBilletById(b.getId());
        System.out.println("Apres scan : statut = " + scanne.getStatut()
                + ", agent = " + scanne.getAgentId());

        // 5. Billet deja utilise
        essayer(() -> service.scannerBillet(b.getCodeQR(), 7));
        essayer(() -> service.annulerBillet(b.getId()));

        // 6. Nettoyage (les billets sont supprimes avec l'evenement : CASCADE)
        evenementService.deleteEvenement(futur.getId());
        evenementService.deleteEvenement(passe.getId());
        System.out.println("Nettoyage termine.");
    }
}