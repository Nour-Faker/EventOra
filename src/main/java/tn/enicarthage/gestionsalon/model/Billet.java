package tn.enicarthage.gestionsalon.model;

import java.time.LocalDateTime;

public class Billet {

    private int id;
    private int visiteurId;
    private int evenementId;
    private Integer agentId;
    private String codeQR;
    private LocalDateTime dateAchat;
    private StatutBillet statut;

    public Billet() {
    }

    public Billet(int id, int visiteurId, int evenementId, Integer agentId,
                  String codeQR, LocalDateTime dateAchat, StatutBillet statut) {
        this.id = id;
        this.visiteurId = visiteurId;
        this.evenementId = evenementId;
        this.agentId = agentId;
        this.codeQR = codeQR;
        this.dateAchat = dateAchat;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVisiteurId() { return visiteurId; }
    public void setVisiteurId(int visiteurId) { this.visiteurId = visiteurId; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public Integer getAgentId() { return agentId; }
    public void setAgentId(Integer agentId) { this.agentId = agentId; }

    public String getCodeQR() { return codeQR; }
    public void setCodeQR(String codeQR) { this.codeQR = codeQR; }

    public LocalDateTime getDateAchat() { return dateAchat; }
    public void setDateAchat(LocalDateTime dateAchat) { this.dateAchat = dateAchat; }

    public StatutBillet getStatut() { return statut; }
    public void setStatut(StatutBillet statut) { this.statut = statut; }
}