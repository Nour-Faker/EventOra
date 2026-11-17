package tn.enicarthage.gestionsalon.util;

import tn.enicarthage.gestionsalon.model.Role;

/** Identité extraite du token. C'est la SEULE source de l'id utilisateur dans les controllers. */
public record AuthUser(int id, Role role) {}