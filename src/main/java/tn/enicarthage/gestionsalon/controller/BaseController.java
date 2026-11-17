package tn.enicarthage.gestionsalon.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tn.enicarthage.gestionsalon.exception.*;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.util.AuthUser;
import tn.enicarthage.gestionsalon.util.JwtUtil; // ADAPTEZ : votre classe JWT

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Socle commun des controllers.
 * - Traduit VOS exceptions métier en codes HTTP (un seul endroit).
 * - Lit l'utilisateur depuis le token, jamais depuis le body.
 * Hypothèse : vos exceptions étendent RuntimeException et ont un constructeur (String).
 * Si EvenementController a déjà son propre socle, gardez le vôtre et reprenez
 * seulement le bloc catch de handle().
 */
public abstract class BaseController implements HttpHandler {

    private static final System.Logger LOG = System.getLogger("api");

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // dates AAAA-MM-JJ

    protected abstract void route(HttpExchange ex) throws Exception;

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            addCors(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            route(ex);

            // ----- Faute de l'appelant (4xx) : pas de log ERROR, message écrit par nous -----
        } catch (AuthenticationException e)    { error(ex, 401, "NON_AUTHENTIFIE", e.getMessage());
        } catch (ForbiddenException e)         { error(ex, 403, "INTERDIT", e.getMessage());
        } catch (ResourceNotFoundException e)  { error(ex, 404, "INTROUVABLE", e.getMessage());
        } catch (ConflictException e)          { error(ex, 409, "CONFLIT", e.getMessage());
        } catch (IllegalArgumentException e)   { error(ex, 400, "REQUETE_INVALIDE", e.getMessage());

            // ----- Faute du système (5xx) : log complet, message générique pour le client -----
        } catch (DataAccessException e)        { serverError(ex, e);
        } catch (Exception e)                  { serverError(ex, e);
        }
    }

    // ---------- Réponses ----------
    protected void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void error(HttpExchange ex, int status, String code, String message) throws IOException {
        sendJson(ex, status, Map.of("erreur", message == null ? "" : message, "code", code));
    }

    private void serverError(HttpExchange ex, Exception e) throws IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        LOG.log(System.Logger.Level.ERROR,
                "requestId=" + requestId + " " + ex.getRequestMethod() + " " + ex.getRequestURI().getPath(), e);
        error(ex, 500, "ERREUR_INTERNE", "Erreur interne (réf. " + requestId + ")");
    }

    /** Appelée dans le `default` d'un switch sur la méthode HTTP. */
    protected void methodNotAllowed(HttpExchange ex) throws IOException {
        error(ex, 405, "METHODE_NON_SUPPORTEE", "Méthode non supportée");
    }

    // ---------- Entrée ----------
    protected <T> T readBody(HttpExchange ex, Class<T> type) throws IOException {
        try {
            T body = MAPPER.readValue(ex.getRequestBody(), type);
            if (body == null) throw new IllegalArgumentException("Corps de requête manquant");
            return body;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON invalide");
        }
    }

    /** "/stand-requests/7/validate" -> ["stand-requests","7","validate"] */
    protected String[] segments(HttpExchange ex) {
        return Arrays.stream(ex.getRequestURI().getPath().split("/"))
                .filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    protected int parseId(String raw) {
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Identifiant invalide"); }
    }

    // ---------- Auth ----------
    protected AuthUser requireAuth(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        if (h == null || !h.startsWith("Bearer "))
            throw new AuthenticationException("Token manquant");
        try {
            return JwtUtil.verify(h.substring(7)); // ADAPTEZ : doit renvoyer AuthUser(id, role)
        } catch (Exception e) {
            throw new AuthenticationException("Token invalide ou expiré");
        }
    }

    protected AuthUser requireRole(HttpExchange ex, Role... allowed) {
        AuthUser u = requireAuth(ex);
        if (!Arrays.asList(allowed).contains(u.role()))
            throw new ForbiddenException("Accès refusé");
        return u;
    }

    private void addCors(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin",
                System.getenv().getOrDefault("CORS_ORIGIN", "http://localhost:5173"));
        h.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }
}