package tn.enicarthage.gestionsalon.exception;

/** La ressource existe mais ne supporte pas cette methode HTTP : 405 + en-tete Allow. */
public class MethodNotAllowedException extends RuntimeException {

    private final String allow;

    public MethodNotAllowedException(String methode, String chemin, String allow) {
        super("Methode non supportee : " + methode + " " + chemin + " (autorisees : " + allow + ").");
        this.allow = allow;
    }

    public String getAllow() {
        return allow;
    }
}
