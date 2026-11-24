package tn.enicarthage.gestionsalon.dto;

import tn.enicarthage.gestionsalon.model.Session;

import java.time.LocalDateTime;

public record SessionResponse(int id, int evenementId, String titre, String description,
                              LocalDateTime heureDebut, LocalDateTime heureFin,
                              int capaciteMax, int placesRestantes) {

    public SessionResponse(Session s, int placesRestantes) {
        this(s.getId(), s.getEvenementId(), s.getTitre(), s.getDescription(),
                s.getHeureDebut(), s.getHeureFin(), s.getCapaciteMax(), placesRestantes);
    }
}