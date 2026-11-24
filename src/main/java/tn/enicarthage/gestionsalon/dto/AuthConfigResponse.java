package tn.enicarthage.gestionsalon.dto;

// GET /auth/config : le front adapte l'inscription et cache "mot de passe oublie" si emailActif = false
public record AuthConfigResponse(boolean emailActif) {}
