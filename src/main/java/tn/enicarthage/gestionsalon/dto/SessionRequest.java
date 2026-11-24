package tn.enicarthage.gestionsalon.dto;

import java.time.LocalDateTime;

public record SessionRequest(String titre, String description,
                             LocalDateTime heureDebut, LocalDateTime heureFin,
                             int capaciteMax) {
}