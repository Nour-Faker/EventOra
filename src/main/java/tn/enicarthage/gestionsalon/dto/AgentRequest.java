package tn.enicarthage.gestionsalon.dto;

public record AgentRequest(String nom, String prenom, String email,
                           String motDePasse, String telephone, String matricule) {
}