package tn.enicarthage.gestionsalon.model;

import java.time.LocalDateTime;

public class Session {

    private int id;
    private int evenementId;
    private String titre;
    private String description;
    private LocalDateTime heureDebut;
    private LocalDateTime heureFin;
    private int capaciteMax;

    public Session() {
    }

    public Session(int id, int evenementId, String titre, String description,
                   LocalDateTime heureDebut, LocalDateTime heureFin,
                   int capaciteMax) {
        this.id = id;
        this.evenementId = evenementId;
        this.titre = titre;
        this.description = description;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.capaciteMax = capaciteMax;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalDateTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalDateTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalDateTime heureFin) { this.heureFin = heureFin; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }
}