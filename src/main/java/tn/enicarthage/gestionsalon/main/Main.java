package tn.enicarthage.gestionsalon.main;

import tn.enicarthage.gestionsalon.model.Evenement;
import tn.enicarthage.gestionsalon.model.Session;
import tn.enicarthage.gestionsalon.service.EvenementService;
import tn.enicarthage.gestionsalon.service.SessionService;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Main {

    // Execute une action et affiche le refus si une regle metier l'interdit
    private static void essayer(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.println("Refuse : " + e.getMessage());
        }
    }

    private static Session nouvelleSession(int evenementId, String titre,
                                           LocalDateTime debut, LocalDateTime fin,
                                           int capacite) {
        Session s = new Session();
        s.setEvenementId(evenementId);
        s.setTitre(titre);
        s.setDescription("Session de test");
        s.setHeureDebut(debut);
        s.setHeureFin(fin);
        s.setCapaciteMax(capacite);
        return s;
    }

    public static void main(String[] args) {
        EvenementService evenementService = new EvenementService();
        SessionService service = new SessionService();

        // Evenement de test : du 10 au 11 mars 2027
        Evenement e = new Evenement();
        e.setTitre("Evenement session test");
        e.setDescription("Evenement de test");
        e.setDateDebut(LocalDate.of(2027, 3, 10));
        e.setDateFin(LocalDate.of(2027, 3, 11));
        e.setLieu("Sfax");
        evenementService.createEvenement(e);
        System.out.println("Evenement de test cree : " + e.getId());

        LocalDateTime debut = LocalDateTime.of(2027, 3, 10, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2027, 3, 10, 11, 30);

        // 1. Sessions refusees
        essayer(() -> service.createSession(
                nouvelleSession(e.getId(), "Test", fin, debut, 1)));            // fin avant debut
        essayer(() -> service.createSession(nouvelleSession(e.getId(), "Test",
                LocalDateTime.of(2027, 4, 1, 10, 0),
                LocalDateTime.of(2027, 4, 1, 11, 0), 1)));                      // hors evenement
        essayer(() -> service.createSession(
                nouvelleSession(e.getId(), "Test", debut, fin, 0)));            // capacite nulle

        // 2. Session valide avec une seule place
        Session s = nouvelleSession(e.getId(), "Session de test", debut, fin, 1);
        service.createSession(s);
        System.out.println("Session creee, id = " + s.getId()
                + ", debut = " + service.getSessionById(s.getId()).getHeureDebut()
                + ", places restantes = " + service.getPlacesRestantes(s.getId()));

        // 3. Inscriptions
        service.inscrireVisiteur(5, s.getId());
        System.out.println("Visiteur 5 inscrit. Places restantes : "
                + service.getPlacesRestantes(s.getId()));

        essayer(() -> service.inscrireVisiteur(5, s.getId()));    // deja inscrit
        essayer(() -> service.inscrireVisiteur(6, s.getId()));    // session complete
        essayer(() -> service.inscrireVisiteur(2, s.getId()));    // c'est un exposant
        essayer(() -> service.inscrireVisiteur(999, s.getId()));  // visiteur inconnu
        essayer(() -> service.inscrireVisiteur(6, 999));          // session inconnue

        // 4. Desinscription, puis inscription du visiteur 6
        service.desinscrireVisiteur(5, s.getId());
        System.out.println("Apres desinscription du visiteur 5 : places restantes = "
                + service.getPlacesRestantes(s.getId()));
        essayer(() -> service.desinscrireVisiteur(5, s.getId())); // n'est plus inscrit

        service.inscrireVisiteur(6, s.getId());
        System.out.println("Visiteur 6 inscrit. Places restantes : "
                + service.getPlacesRestantes(s.getId()));

        // 5. Les sessions du visiteur 6 (JOIN sur la table intermediaire)
        System.out.println("--- Sessions du visiteur 6 ---");
        for (Session x : service.getSessionsByVisiteur(6)) {
            System.out.println("- " + x.getTitre() + " (" + x.getHeureDebut() + ")");
        }

        // 6. Nettoyage (sessions et inscriptions supprimees avec l'evenement : CASCADE)
        evenementService.deleteEvenement(e.getId());
        System.out.println("Nettoyage termine.");
    }
}