package tn.enicarthage.gestionsalon.model;

public abstract class Utilisateur {

    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasseHash;
    private String telephone;
    private Role role;
    private boolean estBloque;

    public Utilisateur() {
    }

    public Utilisateur(int id, String nom, String prenom, String email,
                       String motDePasseHash, String telephone,
                       Role role, boolean estBloque) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
        this.telephone = telephone;
        this.role = role;
        this.estBloque = estBloque;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEstBloque() { return estBloque; }
    public void setEstBloque(boolean estBloque) { this.estBloque = estBloque; }
}