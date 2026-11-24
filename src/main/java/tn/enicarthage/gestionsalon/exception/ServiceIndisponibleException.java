package tn.enicarthage.gestionsalon.exception;

/** Un service externe (envoi d'email via Resend) n'a pas repondu correctement : 503. */
public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message) {
        super(message);
    }
}
