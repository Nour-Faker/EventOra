package tn.enicarthage.gestionsalon.model;

public class Visiteur extends Utilisateur {

    public Visiteur() {
        setRole(Role.VISITEUR);
    }

    public Visiteur(int id, String nom, String prenom, String email,
                    String motDePasseHash, String telephone,
                    boolean estBloque) {
        super(id, nom, prenom, email, motDePasseHash, telephone,
                Role.VISITEUR, estBloque);
    }
}