package tn.enicarthage.gestionSalon.model;

public class Administrateur extends Utilisateur {

    public Administrateur() {
        setRole(Role.ADMINISTRATEUR);
    }

    public Administrateur(int id, String nom, String prenom, String email,
                          String motDePasseHash, String telephone,
                          boolean estBloque) {
        super(id, nom, prenom, email, motDePasseHash, telephone,
                Role.ADMINISTRATEUR, estBloque);
    }
}