package tn.enicarthage.gestionsalon.dto;

public record ResetPasswordRequest(String email, String code, String nouveauMotDePasse) {}
