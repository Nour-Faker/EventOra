package tn.enicarthage.gestionsalon.dto;

import java.math.BigDecimal;

// L'evenement vient de l'URL (POST /events/{id}/ticket-types)
public record TypeBilletRequest(String nom, BigDecimal prix, Integer quota) {}
