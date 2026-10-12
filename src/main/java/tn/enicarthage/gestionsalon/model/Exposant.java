package tn.enicarthage.gestionsalon.model;

public class Exposant extends Utilisateur {

    private String nomEntreprise;
    private String logoUrl;
    private String description;

    public Exposant() {
        setRole(Role.EXPOSANT);
    }

    public Exposant(int id, String nom, String prenom, String email,
                    String motDePasseHash, String telephone, boolean estBloque,
                    String nomEntreprise, String logoUrl, String description) {
        super(id, nom, prenom, email, motDePasseHash, telephone,
                Role.EXPOSANT, estBloque);
        this.nomEntreprise = nomEntreprise;
        this.logoUrl = logoUrl;
        this.description = description;
    }

    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }
}