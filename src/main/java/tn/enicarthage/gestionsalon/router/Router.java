package tn.enicarthage.gestionsalon.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.HttpExchange;

import tn.enicarthage.gestionsalon.controller.AdminController;
import tn.enicarthage.gestionsalon.controller.AuthController;
import tn.enicarthage.gestionsalon.controller.BilletController;
import tn.enicarthage.gestionsalon.controller.EvenementController;
import tn.enicarthage.gestionsalon.controller.StandController;
import tn.enicarthage.gestionsalon.dto.AchatRequest;
import tn.enicarthage.gestionsalon.dto.BlocageRequest;
import tn.enicarthage.gestionsalon.dto.CheckinRequest;
import tn.enicarthage.gestionsalon.dto.DecisionRequest;
import tn.enicarthage.gestionsalon.dto.DemandeStandRequest;
import tn.enicarthage.gestionsalon.dto.EvenementRequest;
import tn.enicarthage.gestionsalon.dto.LoginRequest;
import tn.enicarthage.gestionsalon.dto.RegisterRequest;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ForbiddenException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {

    private static final String ORIGINE_AUTORISEE = "http://localhost:5173";
    private static final int TAILLE_MAX_BODY = 64 * 1024;
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());

    private record Reponse(int statut, String corps) {
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class,
                    (JsonSerializer<LocalDate>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDate.class,
                    (JsonDeserializer<LocalDate>) (json, type, ctx) -> {
                        if (json.isJsonNull()) {
                            return null;
                        }
                        try {
                            return LocalDate.parse(json.getAsString());
                        } catch (DateTimeParseException e) {
                            throw new IllegalArgumentException(
                                    "Date invalide : " + json.getAsString() + " (format attendu : AAAA-MM-JJ)");
                        }
                    })
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> {
                        if (json.isJsonNull()) {
                            return null;
                        }
                        try {
                            return LocalDateTime.parse(json.getAsString());
                        } catch (DateTimeParseException e) {
                            throw new IllegalArgumentException(
                                    "Date-heure invalide : " + json.getAsString()
                                            + " (format attendu : AAAA-MM-JJTHH:MM:SS)");
                        }
                    })
            .disableHtmlEscaping()
            .create();

    private final AuthController authController = new AuthController();
    private final EvenementController evenementController = new EvenementController();
    private final BilletController billetController = new BilletController();
    private final StandController standController = new StandController();
    private final AdminController adminController = new AdminController();

    // En-tetes CORS poses sur TOUTES les reponses (200, 401, 403, 404, 409, 500...)
    private void ajouterCors(HttpExchange exchange) {
        var h = exchange.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", ORIGINE_AUTORISEE);
        h.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Access-Control-Max-Age", "86400");
        h.set("Vary", "Origin");
    }

    public void dispatcher(HttpExchange exchange) throws IOException {
        ajouterCors(exchange);

        String methode = exchange.getRequestMethod();
        String chemin = exchange.getRequestURI().getPath();

        // Requete preliminaire CORS (preflight)
        if ("OPTIONS".equals(methode)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String[] segments = chemin.split("/");

        try {
            Reponse reponse = router(methode, segments, exchange);
            envoyer(exchange, reponse.statut(), reponse.corps());
        } catch (AuthenticationException e) {
            envoyer(exchange, 401, messageErreur(e));
        } catch (ForbiddenException e) {
            envoyer(exchange, 403, messageErreur(e));
        } catch (ResourceNotFoundException e) {
            envoyer(exchange, 404, messageErreur(e));
        } catch (ConflictException e) {
            envoyer(exchange, 409, messageErreur(e));
        } catch (IllegalArgumentException e) {
            envoyer(exchange, 400, messageErreur(e));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur interne sur " + methode + " " + chemin, e);
            envoyer(exchange, 500, gson.toJson(Map.of("erreur", "Erreur interne du serveur.")));
        }
    }

    private Reponse router(String methode, String[] segments, HttpExchange exchange) throws IOException {
        // GET / : healthcheck ("/".split("/") donne un tableau vide en Java)
        if (segments.length == 0 && "GET".equals(methode)) {
            return new Reponse(200, gson.toJson(Map.of("service", "gestion-salon", "statut", "ok")));
        }
        if (segments.length < 2 || segments[1].isBlank()) {
            throw new ResourceNotFoundException("Route vide ou inconnue.");
        }

        switch (segments[1]) {
            case "events":
                return routesEvenement(methode, segments, exchange);
            case "auth":
                return routesAuth(methode, segments, exchange);
            case "tickets":
                return routesTickets(methode, segments, exchange);
            case "me":
                return routesMe(methode, segments, exchange);
            case "checkin":
                return routesCheckin(methode, segments, exchange);
            case "stand-requests":
                return routesDemandesStand(methode, segments, exchange);
            case "users":
                return routesUsers(methode, segments, exchange);
            case "stats":
                return routesStats(methode, segments, exchange);
            default:
                throw new ResourceNotFoundException("Route inconnue : /" + segments[1]);
        }
    }

    // GET /events | GET /events/{id} | POST /events | PUT /events/{id} | DELETE /events/{id}
    // GET /events/{id}/ticket-types (public) | GET /events/{id}/stands (exposant, admin)
    private Reponse routesEvenement(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            switch (methode) {
                case "GET":
                    return new Reponse(200, gson.toJson(evenementController.lister()));
                case "POST": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    EvenementRequest request = lireCorps(exchange, EvenementRequest.class);
                    validerEvenement(request);
                    return new Reponse(201, gson.toJson(evenementController.creer(request)));
                }
                default:
                    throw new IllegalArgumentException("Methode non supportee : " + methode + " /events");
            }
        }

        if (segments.length == 3) {
            int id = entier(segments[2]);
            switch (methode) {
                case "GET":
                    return new Reponse(200, gson.toJson(evenementController.obtenir(id)));
                case "PUT": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    EvenementRequest request = lireCorps(exchange, EvenementRequest.class);
                    validerEvenement(request);
                    return new Reponse(200, gson.toJson(evenementController.modifier(id, request)));
                }
                case "DELETE":
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    evenementController.supprimer(id);
                    return new Reponse(204, "");
                default:
                    throw new IllegalArgumentException("Methode non supportee : " + methode + " /events/{id}");
            }
        }

        if (segments.length == 4 && "GET".equals(methode)) {
            if ("ticket-types".equals(segments[3])) {
                return new Reponse(200, gson.toJson(billetController.types(entier(segments[2]))));
            }
            if ("stands".equals(segments[3])) {
                exigerRole(exchange, Role.EXPOSANT.name(), Role.ADMINISTRATEUR.name());
                return new Reponse(200, gson.toJson(standController.stands(entier(segments[2]))));
            }
        }

        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // POST /auth/login | POST /auth/register
    private Reponse routesAuth(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length != 3 || !"POST".equals(methode)) {
            throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
        }
        switch (segments[2]) {
            case "login":
                return new Reponse(200, gson.toJson(
                        authController.login(lireCorps(exchange, LoginRequest.class))));
            case "register":
                return new Reponse(201, gson.toJson(
                        authController.register(lireCorps(exchange, RegisterRequest.class))));
            default:
                throw new ResourceNotFoundException("Route inconnue : /auth/" + segments[2]);
        }
    }

    // POST /tickets | DELETE /tickets/{id}   (visiteur uniquement)
    private Reponse routesTickets(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2 && "POST".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
            AchatRequest r = lireCorps(exchange, AchatRequest.class);
            return new Reponse(201, gson.toJson(
                    billetController.acheter(claims.userId(), exiger(r.typeBilletId(), "typeBilletId"))));
        }
        if (segments.length == 3 && "DELETE".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
            billetController.annuler(claims.userId(), entier(segments[2]));
            return new Reponse(204, "");
        }
        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // GET /me/tickets (visiteur) | GET /me/stand-requests (exposant)
    private Reponse routesMe(String methode, String[] segments, HttpExchange exchange) {
        if (segments.length == 3 && "GET".equals(methode)) {
            if ("tickets".equals(segments[2])) {
                JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
                return new Reponse(200, gson.toJson(billetController.mesBillets(claims.userId())));
            }
            if ("stand-requests".equals(segments[2])) {
                JwtUtil.Claims claims = exigerRole(exchange, Role.EXPOSANT.name());
                return new Reponse(200, gson.toJson(standController.mesDemandes(claims.userId())));
            }
        }
        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // POST /checkin   (agent d'accueil uniquement : billet.agent_id reference agent_accueil)
    private Reponse routesCheckin(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length != 2 || !"POST".equals(methode)) {
            throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
        }
        JwtUtil.Claims claims = exigerRole(exchange, Role.AGENT_ACCUEIL.name());
        CheckinRequest r = lireCorps(exchange, CheckinRequest.class);
        return new Reponse(200, gson.toJson(
                billetController.scanner(claims.userId(), exiger(r.code(), "code"))));
    }

    // POST /stand-requests (exposant) | GET /stand-requests (admin) | PUT /stand-requests/{id} (admin)
    private Reponse routesDemandesStand(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2 && "POST".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.EXPOSANT.name());
            DemandeStandRequest r = lireCorps(exchange, DemandeStandRequest.class);
            return new Reponse(201, gson.toJson(
                    standController.demander(claims.userId(), exiger(r.standId(), "standId"))));
        }
        if (segments.length == 2 && "GET".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(200, gson.toJson(standController.toutesLesDemandes()));
        }
        if (segments.length == 3 && "PUT".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.ADMINISTRATEUR.name());
            DecisionRequest r = lireCorps(exchange, DecisionRequest.class);
            return new Reponse(200, gson.toJson(
                    standController.traiter(claims.userId(), entier(segments[2]),
                            exiger(r.decision(), "decision"), r.motif())));
        }
        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // GET /users | PUT /users/{id}/block   (admin)
    private Reponse routesUsers(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2 && "GET".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(200, gson.toJson(adminController.utilisateurs()));
        }
        if (segments.length == 4 && "block".equals(segments[3]) && "PUT".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.ADMINISTRATEUR.name());
            BlocageRequest r = lireCorps(exchange, BlocageRequest.class);
            adminController.bloquer(claims.userId(), entier(segments[2]), exiger(r.bloque(), "bloque"));
            return new Reponse(204, "");
        }
        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // GET /stats   (admin)
    private Reponse routesStats(String methode, String[] segments, HttpExchange exchange) {
        if (segments.length == 2 && "GET".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(200, gson.toJson(adminController.stats()));
        }
        throw new ResourceNotFoundException("Route inconnue : /" + String.join("/", segments));
    }

    // Lit "Authorization: Bearer <jeton>", verifie le jeton, l'etat du compte ET que le role du jeton
    // correspond toujours au role en base (un changement de role invalide les anciens jetons)
    private JwtUtil.Claims authentifier(HttpExchange exchange) {
        String entete = exchange.getRequestHeaders().getFirst("Authorization");
        if (entete == null || !entete.startsWith("Bearer ")) {
            throw new AuthenticationException("Authentification requise.");
        }
        JwtUtil.Claims claims;
        try {
            claims = JwtUtil.verifier(entete.substring(7).trim());
        } catch (Exception e) {
            // Jeton malforme, signature invalide, expire... -> 401 et non 400
            throw new AuthenticationException("Jeton invalide ou expire.");
        }
        String roleBase = adminController.roleActif(claims.userId());
        if (roleBase == null) {
            throw new AuthenticationException("Compte bloque ou inexistant.");
        }
        if (!roleBase.equals(claims.role())) {
            throw new AuthenticationException("Jeton obsolete, reconnectez-vous.");
        }
        return claims;
    }

    // Authentifie puis verifie le role (le role vient du jeton, jamais du body)
    private JwtUtil.Claims exigerRole(HttpExchange exchange, String... rolesAutorises) {
        JwtUtil.Claims claims = authentifier(exchange);
        for (String role : rolesAutorises) {
            if (role.equals(claims.role())) {
                return claims;
            }
        }
        throw new ForbiddenException("Acces refuse pour le role " + claims.role() + ".");
    }

    // Verifie qu'un champ obligatoire est present (null ou chaine vide -> 400)
    private <T> T exiger(T valeur, String champ) {
        if (valeur == null || (valeur instanceof String s && s.isBlank())) {
            throw new IllegalArgumentException("Champ obligatoire manquant : " + champ);
        }
        return valeur;
    }

    // Champs NOT NULL de la table evenement : titre, date_debut, date_fin, lieu
    private void validerEvenement(EvenementRequest request) {
        exiger(request.titre(), "titre");
        exiger(request.lieu(), "lieu");
        exiger(request.dateDebut(), "dateDebut");
        exiger(request.dateFin(), "dateFin");
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new IllegalArgumentException("dateFin doit etre apres dateDebut.");
        }
    }

    private <T> T lireCorps(HttpExchange exchange, Class<T> type) throws IOException {
        byte[] octets = exchange.getRequestBody().readNBytes(TAILLE_MAX_BODY + 1);
        if (octets.length > TAILLE_MAX_BODY) {
            throw new IllegalArgumentException("Corps de requete trop volumineux (max 64 Ko).");
        }
        String corps = new String(octets, StandardCharsets.UTF_8);
        if (corps.isBlank()) {
            throw new IllegalArgumentException("Corps de requete vide.");
        }
        try {
            T objet = gson.fromJson(corps, type);
            if (objet == null) {
                throw new IllegalArgumentException("Corps de requete vide.");
            }
            return objet;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("JSON invalide.");
        }
    }

    private int entier(String valeur) {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID invalide : " + valeur);
        }
    }

    private String messageErreur(Exception e) {
        String msg = e.getMessage();
        return gson.toJson(Map.of("erreur", msg != null ? msg : "Erreur inconnue."));
    }

    private void envoyer(HttpExchange exchange, int statut, String corps) throws IOException {
        byte[] bytes = corps == null ? new byte[0] : corps.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 0) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statut, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(statut, -1);
            exchange.close();
        }
    }
}