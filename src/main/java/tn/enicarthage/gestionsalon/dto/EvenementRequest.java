package tn.enicarthage.gestionsalon.dto;

import java.time.LocalDate;

public record EvenementRequest(String titre, String description,
                               LocalDate dateDebut, LocalDate dateFin, String lieu) {
}