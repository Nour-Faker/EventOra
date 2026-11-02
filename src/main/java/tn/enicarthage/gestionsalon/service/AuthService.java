package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;
import tn.enicarthage.gestionsalon.model.Exposant;
import tn.enicarthage.gestionsalon.model.Utilisateur;
import tn.enicarthage.gestionsalon.model.Visiteur;
import tn.enicarthage.gestionsalon.util.PasswordUtil;

public class AuthService {

    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UtilisateurDAO utilisateurDAO;

    public AuthService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }

    public Visiteur inscrireVisiteur(String nom, String prenom, String email,
                                     String motDePasse, String telephone) {
        String emailNormalise = validerInscription(nom, prenom, email, motDePasse);

        Visiteur visiteur = new Visiteur();
        visiteur.setNom(nom.trim());
        visiteur.setPrenom(prenom.trim());
        visiteur.setEmail(emailNormalise);
        visiteur.setMotDePasseHash(PasswordUtil.hash(motDePasse));
        visiteur.setTelephone(telephone);
        utilisateurDAO.create(visiteur);
        return visiteur;
    }

    public Exposant inscrireExposant(String nom, String prenom, String email,
                                     String motDePasse, String telephone,
                                     String nomEntreprise, String description) {
        String emailNormalise = validerInscription(nom, prenom, email, motDePasse);
        if (nomEntreprise == null || nomEntreprise.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire.");
        }

        Exposant exposant = new Exposant();
        exposant.setNom(nom.trim());
        exposant.setPrenom(prenom.trim());
        exposant.setEmail(emailNormalise);
        exposant.setMotDePasseHash(PasswordUtil.hash(motDePasse));
        exposant.setTelephone(telephone);
        exposant.setNomEntreprise(nomEntreprise.trim());
        exposant.setDescription(description);
        utilisateurDAO.createExposant(exposant);
        return exposant;
    }

    public Utilisateur seConnecter(String email, String motDePasse) {
        String message = "Email ou mot de passe incorrect.";

        if (email == null || motDePasse == null) {
            throw new AuthenticationException(message);
        }

        Utilisateur utilisateur = utilisateurDAO.findByEmail(email.trim().toLowerCase());
        if (utilisateur == null
                || !PasswordUtil.verify(motDePasse, utilisateur.getMotDePasseHash())) {
            throw new AuthenticationException(message);
        }
        if (utilisateur.isEstBloque()) {
            throw new AuthenticationException("Ce compte est bloque.");
        }

        return utilisateur;
    }

    // Verifie les champs communs et renvoie l'email normalise
    private String validerInscription(String nom, String prenom, String email,
                                      String motDePasse) {
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le nom et le prenom sont obligatoires.");
        }
        if (email == null || !email.trim().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("L'adresse email est invalide.");
        }
        if (motDePasse == null || motDePasse.length() < 8) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins 8 caracteres.");
        }

        String emailNormalise = email.trim().toLowerCase();
        if (utilisateurDAO.existsByEmail(emailNormalise)) {
            throw new IllegalArgumentException("Cette adresse email est deja utilisee.");
        }
        return emailNormalise;
    }
}