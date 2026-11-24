package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.CodeEmailDAO;
import tn.enicarthage.gestionsalon.dao.CodeEmailDAO.CodeActif;
import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ServiceIndisponibleException;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.util.CodeUtil;
import tn.enicarthage.gestionsalon.util.PasswordUtil;

/**
 * Verification de l'adresse email a l'inscription et mot de passe oublie, avec un code a 6 chiffres.
 * Regles : code valable 15 minutes, 5 essais maximum, un seul code actif par usage,
 * 60 secondes minimum entre deux envois.
 * "Renvoyer le code" et "mot de passe oublie" repondent toujours pareil, que l'email existe ou non :
 * on ne revele pas quelles adresses ont un compte.
 */
public class VerificationService {

    public static final int DUREE_MINUTES = 15;
    private static final int MAX_TENTATIVES = 5;
    private static final int DELAI_RENVOI_SECONDES = 60;
    private static final String CODE_INVALIDE = "Code expire ou invalide. Demandez un nouveau code.";

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final CodeEmailDAO codeDAO = new CodeEmailDAO();
    private final EmailService emailService = new EmailService();

    // Appele juste apres l'inscription
    public void envoyerCodeVerification(Utilisateur u) {
        envoyer(u, CodeEmailDAO.VERIFICATION);
    }

    public void renvoyerCodeVerification(String email) {
        if (!EmailService.actif()) {
            throw new ServiceIndisponibleException("La verification par email n'est pas activee.");
        }
        Utilisateur u = trouver(email);
        if (u == null || u.isEmailVerifie() || u.isEstBloque()) return;
        envoyer(u, CodeEmailDAO.VERIFICATION);
    }

    // Verifie le code et active le compte ; renvoie l'utilisateur pour le connecter directement
    public Utilisateur verifierEmail(String email, String code) {
        if (!EmailService.actif()) {
            throw new ServiceIndisponibleException("La verification par email n'est pas activee.");
        }
        Utilisateur u = trouver(email);
        if (u == null) throw new IllegalArgumentException(CODE_INVALIDE);
        if (u.isEmailVerifie()) throw new ConflictException("Cette adresse est deja verifiee : connectez-vous.");
        controler(u, CodeEmailDAO.VERIFICATION, code);
        utilisateurDAO.marquerEmailVerifie(u.getId());
        u.setEmailVerifie(true);
        return u;
    }

    public void motDePasseOublie(String email) {
        if (!EmailService.actif()) {
            throw new ServiceIndisponibleException("La reinitialisation par email n'est pas activee. Contactez l'administrateur.");
        }
        Utilisateur u = trouver(email);
        if (u == null || u.isEstBloque()) return;
        envoyer(u, CodeEmailDAO.REINITIALISATION);
    }

    public void reinitialiserMotDePasse(String email, String code, String nouveauMotDePasse) {
        if (!EmailService.actif()) {
            throw new ServiceIndisponibleException("La reinitialisation par email n'est pas activee. Contactez l'administrateur.");
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caracteres.");
        }
        Utilisateur u = trouver(email);
        if (u == null || u.isEstBloque()) throw new IllegalArgumentException(CODE_INVALIDE);
        controler(u, CodeEmailDAO.REINITIALISATION, code);
        utilisateurDAO.updateMotDePasse(u.getId(), PasswordUtil.hash(nouveauMotDePasse));
        // Le code recu par email prouve aussi que l'adresse appartient a l'utilisateur
        if (!u.isEmailVerifie()) utilisateurDAO.marquerEmailVerifie(u.getId());
    }

    private Utilisateur trouver(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("L'adresse email est obligatoire.");
        return utilisateurDAO.findByEmail(email.trim().toLowerCase());
    }

    private void envoyer(Utilisateur u, String type) {
        // Anti-spam silencieux : on ne renvoie pas d'erreur (elle revelerait que le compte existe)
        if (codeDAO.envoyeRecemment(u.getId(), type, DELAI_RENVOI_SECONDES)) return;
        String code = CodeUtil.genererCode();
        codeDAO.invaliderTous(u.getId(), type);
        codeDAO.creer(u.getId(), type, CodeUtil.hash(code), DUREE_MINUTES);
        if (CodeEmailDAO.VERIFICATION.equals(type)) {
            emailService.envoyerCodeVerification(u.getEmail(), u.getPrenom(), code, DUREE_MINUTES);
        } else {
            emailService.envoyerCodeReinitialisation(u.getEmail(), u.getPrenom(), code, DUREE_MINUTES);
        }
    }

    private void controler(Utilisateur u, String type, String code) {
        String saisi = code == null ? "" : code.replaceAll("\\s", "");
        if (!saisi.matches("\\d{6}")) throw new IllegalArgumentException("Le code contient 6 chiffres.");

        CodeActif actif = codeDAO.dernierActif(u.getId(), type);
        if (actif == null || actif.expire()) throw new IllegalArgumentException(CODE_INVALIDE);
        if (actif.tentatives() >= MAX_TENTATIVES) {
            throw new IllegalArgumentException("Trop de tentatives. Demandez un nouveau code.");
        }
        if (!CodeUtil.egal(actif.hash(), CodeUtil.hash(saisi))) {
            codeDAO.incrementerTentatives(actif.id());
            int restants = MAX_TENTATIVES - actif.tentatives() - 1;
            throw new IllegalArgumentException(restants > 0
                    ? "Code incorrect. Encore " + restants + " essai" + (restants > 1 ? "s" : "") + "."
                    : "Code incorrect. Demandez un nouveau code.");
        }
        if (!codeDAO.marquerUtilise(actif.id())) throw new IllegalArgumentException(CODE_INVALIDE);
    }
}
