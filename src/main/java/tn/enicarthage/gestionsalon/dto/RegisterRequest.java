package tn.enicarthage.gestionsalon.dto;

public class RegisterRequest {
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String telephone;
    private String role;
    private String nomEntreprise;
    private String description;

    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getTelephone() { return telephone; }
    public String getRole() { return role; }
    public String getNomEntreprise() { return nomEntreprise; }
    public String getDescription() { return description; }
}