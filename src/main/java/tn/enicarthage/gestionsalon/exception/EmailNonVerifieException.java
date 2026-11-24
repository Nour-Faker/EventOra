package tn.enicarthage.gestionsalon.exception;

/** Connexion refusee tant que l'adresse email n'est pas verifiee : 403 + code EMAIL_NON_VERIFIE. */
public class EmailNonVerifieException extends ForbiddenException {
    public EmailNonVerifieException(String message) {
        super(message);
    }
}
