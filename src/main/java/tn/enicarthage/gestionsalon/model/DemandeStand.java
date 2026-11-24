package tn.enicarthage.gestionsalon.model;

import java.time.LocalDateTime;

public class DemandeStand {

    private int id;
    private int exposantId;
    private int standId;
    private Integer adminId;
    private LocalDateTime dateDemande;
    private StatutDemande statut;
    private String motifRefus;

    public DemandeStand() {
    }

    public DemandeStand(int id, int exposantId, int standId, Integer adminId,
                        LocalDateTime dateDemande, StatutDemande statut,
                        String motifRefus) {
        this.id = id;
        this.exposantId = exposantId;
        this.standId = standId;
        this.adminId = adminId;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.motifRefus = motifRefus;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getExposantId() { return exposantId; }
    public void setExposantId(int exposantId) { this.exposantId = exposantId; }

    public int getStandId() { return standId; }
    public void setStandId(int standId) { this.standId = standId; }

    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public StatutDemande getStatut() { return statut; }
    public void setStatut(StatutDemande statut) { this.statut = statut; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }
}