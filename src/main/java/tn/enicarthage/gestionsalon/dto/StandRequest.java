package tn.enicarthage.gestionsalon.dto;

import java.math.BigDecimal;

// L'evenement vient de l'URL (POST /events/{id}/stands) ; le statut est fixe par le serveur
public record StandRequest(Integer categorieId, String numero, String emplacement,
                           BigDecimal superficie, BigDecimal prix) {}
