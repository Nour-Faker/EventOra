package tn.enicarthage.gestionsalon.dto;

/**
 * Reponse de POST /auth/register.
 * - Emails actifs   : verificationRequise = true, pas de jeton (il faut saisir le code recu).
 * - Emails inactifs : verificationRequise = false, token + user fournis (connexion directe).
 */
public record InscriptionResponse(boolean verificationRequise, String email, boolean emailEnvoye,
                                  String token, UtilisateurResponse user) {}
