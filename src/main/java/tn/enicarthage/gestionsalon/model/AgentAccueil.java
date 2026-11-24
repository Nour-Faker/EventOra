package tn.enicarthage.gestionsalon.model;

public class AgentAccueil extends Utilisateur {

    private String matricule;

    public AgentAccueil() {
        setRole(Role.AGENT_ACCUEIL);
    }

    public AgentAccueil(int id, String nom, String prenom, String email,
                        String motDePasseHash, String telephone,
                        boolean estBloque, String matricule) {
        super(id, nom, prenom, email, motDePasseHash, telephone,
                Role.AGENT_ACCUEIL, estBloque);
        this.matricule = matricule;
    }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }
}