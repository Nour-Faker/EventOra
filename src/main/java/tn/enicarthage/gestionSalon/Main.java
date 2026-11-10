package tn.enicarthage.gestionSalon;

import tn.enicarthage.gestionSalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionSalon.model.Exposant;
import tn.enicarthage.gestionSalon.model.Utilisateur;
import tn.enicarthage.gestionSalon.model.Visiteur;
import tn.enicarthage.gestionSalon.service.AuthService;

public class Main {

    // Execute une action et affiche le refus si une regle metier l'interdit
    private static void essayer(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.println("Refuse : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        AuthService service = new AuthService();
        UtilisateurDAO dao = new UtilisateurDAO();

        String suffixe = String.valueOf(System.currentTimeMillis());
        String emailVisiteur = "visiteur" + suffixe + "@mail.tn";
        String emailExposant = "exposant" + suffixe + "@mail.tn";

        // 1. Inscription d'un visiteur
        Visiteur v = service.inscrireVisiteur("Test", "Visiteur", emailVisiteur,
                "motdepasse1", "71000099");
        System.out.println("Visiteur inscrit, id = " + v.getId()
                + ", role = " + v.getRole()
                + ", empreinte hachee = " + v.getMotDePasseHash().startsWith("65536:"));

        // 2. Inscriptions refusees
        essayer(() -> service.inscrireVisiteur("Test", "Visiteur", emailVisiteur,
                "motdepasse1", null));                                    // email deja pris
        essayer(() -> service.inscrireVisiteur("Test", "Visiteur", "pas-un-email",
                "motdepasse1", null));                                    // email invalide
        essayer(() -> service.inscrireVisiteur("Test", "Visiteur", "autre" + suffixe + "@mail.tn",
                "court", null));                                          // mot de passe trop court
        essayer(() -> service.inscrireVisiteur(" ", "Visiteur", "autre" + suffixe + "@mail.tn",
                "motdepasse1", null));                                    // nom vide

        // 3. Inscription d'un exposant (deux tables, une transaction)
        Exposant e = service.inscrireExposant("Test", "Exposant", emailExposant,
                "motdepasse2", "71000098", "Entreprise Test", "Societe de test");
        System.out.println("Exposant inscrit, id = " + e.getId());

        // 4. Connexions reussies
        Utilisateur connecte = service.seConnecter(emailVisiteur, "motdepasse1");
        System.out.println("Connexion : " + connecte.getEmail()
                + " (" + connecte.getRole() + ")");

        Utilisateur connecteExposant = service.seConnecter(emailExposant.toUpperCase(),
                "motdepasse2");
        System.out.println("Connexion : " + connecteExposant.getEmail()
                + " (" + connecteExposant.getRole() + ", "
                + ((Exposant) connecteExposant).getNomEntreprise() + ")");

        // 5. Connexions refusees (meme message pour un email inconnu et un mauvais mot de passe)
        essayer(() -> service.seConnecter(emailVisiteur, "mauvais"));
        essayer(() -> service.seConnecter("inconnu@mail.tn", "motdepasse1"));

        // 6. Un compte de depart (mot de passe provisoire) ne peut pas se connecter
        essayer(() -> service.seConnecter("admin@salon.tn", "motdepasse1"));

        // 7. Compte bloque : refuse, puis debloque : accepte
        dao.setBloque(v.getId(), true);
        essayer(() -> service.seConnecter(emailVisiteur, "motdepasse1"));
        dao.setBloque(v.getId(), false);
        System.out.println("Apres deblocage : "
                + service.seConnecter(emailVisiteur, "motdepasse1").getEmail());

        // 8. Nettoyage (la ligne exposant est supprimee avec l'utilisateur : CASCADE)
        dao.delete(v.getId());
        dao.delete(e.getId());
        System.out.println("Nettoyage termine.");
    }
}