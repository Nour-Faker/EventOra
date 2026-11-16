package tn.enicarthage.gestionsalon.router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import tn.enicarthage.gestionsalon.controller.EvenementController;
import tn.enicarthage.gestionsalon.dto.EvenementRequest;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;
import tn.enicarthage.gestionsalon.exception.ForbiddenException;
import tn.enicarthage.gestionsalon.util.JwtUtil;
import tn.enicarthage.gestionsalon.controller.AuthController;
import tn.enicarthage.gestionsalon.dto.LoginRequest;
import tn.enicarthage.gestionsalon.dto.RegisterRequest;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Router {

    private static final String ORIGINE_AUTORISEE = "http://localhost:5173";
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());
    private final AuthController authController = new AuthController();

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

    private final EvenementController evenementController = new EvenementController();

    private void ajouterCors(HttpExchange exchange) {
        Headers h = exchange.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", ORIGINE_AUTORISEE);
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        h.set("Vary", "Origin");
    }
    public void dispatcher(HttpExchange exchange) throws IOException {
        ajouterCors(exchange);

        String methode = exchange.getRequestMethod();

        if ("OPTIONS".equals(methode)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String chemin = exchange.getRequestURI().getPath();
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
        } catch (IllegalArgumentException e) {
            envoyer(exchange, 400, messageErreur(e));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur interne sur " + methode + " " + chemin, e);
            envoyer(exchange, 500, gson.toJson(Map.of("erreur", "Erreur interne du serveur.")));
        }
    }

    private Reponse router(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length < 2 || segments[1].isBlank()) {
            throw new ResourceNotFoundException("Route vide ou inconnue.");
        }

        switch (segments[1]) {
            case "events":
                return routesEvenement(methode, segments, exchange);
            case "auth":
                return routesAuth(methode, segments, exchange);
            default:
                throw new ResourceNotFoundException("Route inconnue : /" + segments[1]);
        }
    }

    // GET /events | GET /events/{id} | POST /events | PUT /events/{id} | DELETE /events/{id}
    private Reponse routesEvenement(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            switch (methode) {
                case "GET":
                    return new Reponse(200, gson.toJson(evenementController.lister()));
                case "POST": {
                    exigerRole(exchange, "ADMINISTRATEUR");
                    EvenementRequest request = lireCorps(exchange, EvenementRequest.class);
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
                    exigerRole(exchange, "ADMINISTRATEUR");
                    EvenementRequest request = lireCorps(exchange, EvenementRequest.class);
                    return new Reponse(200, gson.toJson(evenementController.modifier(id, request)));
                }
                case "DELETE":
                    exigerRole(exchange, "ADMINISTRATEUR");
                    evenementController.supprimer(id);
                    return new Reponse(204, "");
                default:
                    throw new IllegalArgumentException("Methode non supportee : " + methode + " /events/{id}");
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

    // Lit "Authorization: Bearer <jeton>" et renvoie l'utilisateur connecte
    private JwtUtil.Claims authentifier(HttpExchange exchange) {
        String entete = exchange.getRequestHeaders().getFirst("Authorization");
        if (entete == null || !entete.startsWith("Bearer ")) {
            throw new AuthenticationException("Authentification requise.");
        }
        return JwtUtil.verifier(entete.substring(7).trim());
    }

    // Authentifie puis verifie le role
    private JwtUtil.Claims exigerRole(HttpExchange exchange, String... rolesAutorises) {
        JwtUtil.Claims claims = authentifier(exchange);
        for (String role : rolesAutorises) {
            if (role.equals(claims.role())) {
                return claims;
            }
        }
        throw new ForbiddenException("Acces refuse pour le role " + claims.role() + ".");
    }

    private <T> T lireCorps(HttpExchange exchange, Class<T> type) throws IOException {
        String corps = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
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
        return gson.toJson(Map.of("erreur", String.valueOf(e.getMessage())));
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