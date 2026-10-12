package tn.enicarthage.gestionsalon.model;

import java.math.BigDecimal;

public class Stand {

    private int id;
    private int evenementId;
    private int categorieId;
    private String numero;
    private String emplacement;
    private double superficie;
    private BigDecimal prix;
    private StatutStand statut;

    public Stand() {
    }

    public Stand(int id, int evenementId, int categorieId, String numero,
                 String emplacement, double superficie, BigDecimal prix,
                 StatutStand statut) {
        this.id = id;
        this.evenementId = evenementId;
        this.categorieId = categorieId;
        this.numero = numero;
        this.emplacement = emplacement;
        this.superficie = superficie;
        this.prix = prix;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public int getCategorieId() { return categorieId; }
    public void setCategorieId(int categorieId) { this.categorieId = categorieId; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }

    public double getSuperficie() { return superficie; }
    public void setSuperficie(double superficie) { this.superficie = superficie; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public StatutStand getStatut() { return statut; }
    public void setStatut(StatutStand statut) { this.statut = statut; }
}