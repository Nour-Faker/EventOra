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
import tn.enicarthage.gestionsalon.controller.CategorieController;
import tn.enicarthage.gestionsalon.controller.EvenementController;
import tn.enicarthage.gestionsalon.controller.ProfilController;
import tn.enicarthage.gestionsalon.controller.SessionController;
import tn.enicarthage.gestionsalon.controller.StandController;
import tn.enicarthage.gestionsalon.dto.AchatRequest;
import tn.enicarthage.gestionsalon.dto.AgentRequest;
import tn.enicarthage.gestionsalon.dto.BlocageRequest;
import tn.enicarthage.gestionsalon.dto.CategorieRequest;
import tn.enicarthage.gestionsalon.dto.ChangePasswordRequest;
import tn.enicarthage.gestionsalon.dto.CheckinRequest;
import tn.enicarthage.gestionsalon.dto.DecisionRequest;
import tn.enicarthage.gestionsalon.dto.DemandeStandRequest;
import tn.enicarthage.gestionsalon.dto.EmailRequest;
import tn.enicarthage.gestionsalon.dto.EvenementRequest;
import tn.enicarthage.gestionsalon.dto.LoginRequest;
import tn.enicarthage.gestionsalon.dto.RegisterRequest;
import tn.enicarthage.gestionsalon.dto.ResetPasswordRequest;
import tn.enicarthage.gestionsalon.dto.SessionRequest;
import tn.enicarthage.gestionsalon.dto.StandRequest;
import tn.enicarthage.gestionsalon.dto.TypeBilletRequest;
import tn.enicarthage.gestionsalon.dto.UpdateProfilRequest;
import tn.enicarthage.gestionsalon.dto.VerifyEmailRequest;
import tn.enicarthage.gestionsalon.exception.AuthenticationException;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.EmailNonVerifieException;
import tn.enicarthage.gestionsalon.exception.ForbiddenException;
import tn.enicarthage.gestionsalon.exception.MethodNotAllowedException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.exception.ServiceIndisponibleException;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.util.JwtUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {

    private static final String ORIGINE_AUTORISEE = "http://localhost:5173";
    private static final int TAILLE_MAX_BODY = 64 * 1024;
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());

    // Reponse JSON (corps) ou binaire (PDF du billet)
    private record Reponse(int statut, String corps, byte[] pdf, String nomFichier) {
        Reponse(int statut, String corps) {
            this(statut, corps, null, null);
        }
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

    // Toutes les routes de l'API et leurs methodes : sert a distinguer 404 (chemin inconnu)
    // et 405 (chemin connu, methode non supportee). {id} = segment numerique.
    private static final Map<String, List<String>> ROUTES = new LinkedHashMap<>();

    static {
        route("/auth/config", "GET");
        route("/auth/login", "POST");
        route("/auth/register", "POST");
        route("/auth/verify-email", "POST");
        route("/auth/resend-code", "POST");
        route("/auth/forgot-password", "POST");
        route("/auth/reset-password", "POST");
        route("/me", "GET", "PUT");
        route("/me/password", "PUT");
        route("/me/tickets", "GET");
        route("/me/stand-requests", "GET");
        route("/me/sessions", "GET");
        route("/users", "GET");
        route("/users/agents", "POST");
        route("/users/{id}", "DELETE");
        route("/users/{id}/block", "PUT");
        route("/events", "GET", "POST");
        route("/events/{id}", "GET", "PUT", "DELETE");
        route("/events/{id}/stands", "GET", "POST");
        route("/events/{id}/ticket-types", "GET", "POST");
        route("/events/{id}/sessions", "GET", "POST");
        route("/categories", "GET", "POST");
        route("/categories/{id}", "GET", "PUT", "DELETE");
        route("/stands/{id}", "GET", "PUT", "DELETE");
        route("/stand-requests", "GET", "POST");
        route("/stand-requests/{id}", "PUT");
        route("/ticket-types/{id}", "PUT", "DELETE");
        route("/tickets", "POST");
        route("/tickets/{id}", "DELETE");
        route("/tickets/{id}/pdf", "GET");
        route("/checkin", "POST");
        route("/sessions/{id}", "GET", "PUT", "DELETE");
        route("/sessions/{id}/registration", "POST", "DELETE");
        route("/stats", "GET");
    }

    private static void route(String motif, String... methodes) {
        ROUTES.put(motif, List.of(methodes));
    }

    // "/events/12/stands" -> "/events/{id}/stands"
    private static String motif(String[] segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < segments.length; i++) {
            sb.append('/').append(segments[i].matches("\\d+") ? "{id}" : segments[i]);
        }
        return sb.toString();
    }

    // Chemin connu mais methode absente : 405 avant meme d'entrer dans le groupe de routes
    private void verifierMethode(String methode, String[] segments) {
        String motif = motif(segments);
        List<String> autorisees = ROUTES.get(motif);
        if (autorisees != null && !autorisees.contains(methode)) {
            throw new MethodNotAllowedException(methode, motif, String.join(", ", autorisees));
        }
    }

    private final AuthController authController = new AuthController();
    private final EvenementController evenementController = new EvenementController();
    private final BilletController billetController = new BilletController();
    private final StandController standController = new StandController();
    private final AdminController adminController = new AdminController();
    private final CategorieController categorieController = new CategorieController();
    private final SessionController sessionController = new SessionController();
    private final ProfilController profilController = new ProfilController();

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
            if (reponse.pdf() != null) {
                envoyerPdf(exchange, reponse.pdf(), reponse.nomFichier());
            } else {
                envoyer(exchange, reponse.statut(), reponse.corps());
            }
        } catch (AuthenticationException e) {
            envoyer(exchange, 401, messageErreur(e));
        } catch (EmailNonVerifieException e) {
            envoyer(exchange, 403, gson.toJson(Map.of("erreur", e.getMessage(), "code", "EMAIL_NON_VERIFIE")));
        } catch (ServiceIndisponibleException e) {
            envoyer(exchange, 503, messageErreur(e));
        } catch (ForbiddenException e) {
            envoyer(exchange, 403, messageErreur(e));
        } catch (ResourceNotFoundException e) {
            envoyer(exchange, 404, messageErreur(e));
        } catch (ConflictException e) {
            envoyer(exchange, 409, messageErreur(e));
        } catch (MethodNotAllowedException e) {
            exchange.getResponseHeaders().set("Allow", e.getAllow());
            envoyer(exchange, 405, messageErreur(e));
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
        verifierMethode(methode, segments);

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
            case "categories":
                return routesCategories(methode, segments, exchange);
            case "stands":
                return routesStands(methode, segments, exchange);
            case "ticket-types":
                return routesTypesBillet(methode, segments, exchange);
            case "sessions":
                return routesSessions(methode, segments, exchange);
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
                    throw new MethodNotAllowedException(methode, "/events", "voir README");
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
                    throw new MethodNotAllowedException(methode, "/events/{id}", "voir README");
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
            if ("sessions".equals(segments[3])) {                 // programme (BF-26), public
                return new Reponse(200, gson.toJson(sessionController.programme(entier(segments[2]))));
            }
        }

        // POST /events/{id}/stands | /ticket-types | /sessions   (administrateur)
        if (segments.length == 4 && "POST".equals(methode)) {
            int evenementId = entier(segments[2]);
            switch (segments[3]) {
                case "stands": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    StandRequest r = lireCorps(exchange, StandRequest.class);
                    return new Reponse(201, gson.toJson(standController.creerStand(evenementId,
                            exiger(r.categorieId(), "categorieId"), exiger(r.numero(), "numero"),
                            r.emplacement(), exiger(r.superficie(), "superficie"), exiger(r.prix(), "prix"))));
                }
                case "ticket-types": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    TypeBilletRequest r = lireCorps(exchange, TypeBilletRequest.class);
                    return new Reponse(201, gson.toJson(billetController.creerType(evenementId,
                            exiger(r.nom(), "nom"), exiger(r.prix(), "prix"), exiger(r.quota(), "quota"))));
                }
                case "sessions": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    SessionRequest r = lireCorps(exchange, SessionRequest.class);
                    return new Reponse(201, gson.toJson(sessionController.creer(evenementId, r)));
                }
                default:
                    break;
            }
        }

        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // POST /auth/login | POST /auth/register
    private Reponse routesAuth(String methode, String[] segments, HttpExchange exchange) throws IOException {
        // GET /auth/config : les fonctions par email sont-elles actives ? (public)
        if (segments.length == 3 && "config".equals(segments[2]) && "GET".equals(methode)) {
            return new Reponse(200, gson.toJson(authController.config()));
        }
        if (segments.length != 3 || !"POST".equals(methode)) {
            throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
        }
        switch (segments[2]) {
            case "login":
                return new Reponse(200, gson.toJson(
                        authController.login(lireCorps(exchange, LoginRequest.class))));
            case "register":
                return new Reponse(201, gson.toJson(
                        authController.register(lireCorps(exchange, RegisterRequest.class))));
            case "verify-email": {
                VerifyEmailRequest r = lireCorps(exchange, VerifyEmailRequest.class);
                exiger(r.email(), "email");
                exiger(r.code(), "code");
                return new Reponse(200, gson.toJson(authController.verifierEmail(r)));
            }
            case "resend-code":
                authController.renvoyerCode(exigerEmail(lireCorps(exchange, EmailRequest.class)));
                return new Reponse(204, "");
            case "forgot-password":
                authController.motDePasseOublie(exigerEmail(lireCorps(exchange, EmailRequest.class)));
                return new Reponse(204, "");
            case "reset-password": {
                ResetPasswordRequest r = lireCorps(exchange, ResetPasswordRequest.class);
                exiger(r.email(), "email");
                exiger(r.code(), "code");
                exiger(r.nouveauMotDePasse(), "nouveauMotDePasse");
                authController.reinitialiserMotDePasse(r);
                return new Reponse(204, "");
            }
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
        // GET /tickets/{id}/pdf : PDF de confirmation avec code QR (BF-32), proprietaire uniquement
        if (segments.length == 4 && "pdf".equals(segments[3]) && "GET".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
            int billetId = entier(segments[2]);
            return new Reponse(200, null, billetController.pdf(claims.userId(), billetId),
                    "billet-" + billetId + ".pdf");
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /me/tickets (visiteur) | GET /me/stand-requests (exposant)
    private Reponse routesMe(String methode, String[] segments, HttpExchange exchange) throws IOException {
        // GET /me | PUT /me : profil de l'utilisateur connecte (BF-03, BF-04)
        if (segments.length == 2) {
            JwtUtil.Claims claims = authentifier(exchange);
            switch (methode) {
                case "GET":
                    return new Reponse(200, gson.toJson(profilController.profil(claims.userId())));
                case "PUT":
                    return new Reponse(200, gson.toJson(profilController.modifier(claims.userId(),
                            lireCorps(exchange, UpdateProfilRequest.class))));
                default:
                    throw new MethodNotAllowedException(methode, "/me", "voir README");
            }
        }
        // PUT /me/password
        if (segments.length == 3 && "password".equals(segments[2]) && "PUT".equals(methode)) {
            JwtUtil.Claims claims = authentifier(exchange);
            profilController.changerMotDePasse(claims.userId(), lireCorps(exchange, ChangePasswordRequest.class));
            return new Reponse(204, "");
        }
        if (segments.length == 3 && "GET".equals(methode)) {
            if ("sessions".equals(segments[2])) {                 // sessions du visiteur
                JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
                return new Reponse(200, gson.toJson(sessionController.mesSessions(claims.userId())));
            }
            if ("tickets".equals(segments[2])) {
                JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
                return new Reponse(200, gson.toJson(billetController.mesBillets(claims.userId())));
            }
            if ("stand-requests".equals(segments[2])) {
                JwtUtil.Claims claims = exigerRole(exchange, Role.EXPOSANT.name());
                return new Reponse(200, gson.toJson(standController.mesDemandes(claims.userId())));
            }
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // POST /checkin   (agent d'accueil uniquement : billet.agent_id reference agent_accueil)
    private Reponse routesCheckin(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length != 2 || !"POST".equals(methode)) {
            throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
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
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /users | PUT /users/{id}/block   (admin)
    private Reponse routesUsers(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2 && "GET".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(200, gson.toJson(adminController.utilisateurs()));
        }
        // POST /users/agents : creer un agent d'accueil
        if (segments.length == 3 && "agents".equals(segments[2]) && "POST".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(201, gson.toJson(adminController.creerAgent(lireCorps(exchange, AgentRequest.class))));
        }
        // DELETE /users/{id} (BF-07)
        if (segments.length == 3 && "DELETE".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.ADMINISTRATEUR.name());
            adminController.supprimer(claims.userId(), entier(segments[2]));
            return new Reponse(204, "");
        }
        if (segments.length == 4 && "block".equals(segments[3]) && "PUT".equals(methode)) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.ADMINISTRATEUR.name());
            BlocageRequest r = lireCorps(exchange, BlocageRequest.class);
            adminController.bloquer(claims.userId(), entier(segments[2]), exiger(r.bloque(), "bloque"));
            return new Reponse(204, "");
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /stats   (admin)
    private Reponse routesStats(String methode, String[] segments, HttpExchange exchange) {
        if (segments.length == 2 && "GET".equals(methode)) {
            exigerRole(exchange, Role.ADMINISTRATEUR.name());
            return new Reponse(200, gson.toJson(adminController.stats()));
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /categories, GET /categories/{id} (tout utilisateur connecte)
    // POST /categories, PUT /categories/{id}, DELETE /categories/{id} (administrateur) : BF-08 a BF-10
    private Reponse routesCategories(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 2) {
            switch (methode) {
                case "GET":
                    authentifier(exchange);
                    return new Reponse(200, gson.toJson(categorieController.lister()));
                case "POST": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    CategorieRequest r = lireCorps(exchange, CategorieRequest.class);
                    exiger(r.nom(), "nom");
                    return new Reponse(201, gson.toJson(categorieController.creer(r)));
                }
                default:
                    throw new MethodNotAllowedException(methode, "/categories", "voir README");
            }
        }
        if (segments.length == 3) {
            int id = entier(segments[2]);
            switch (methode) {
                case "GET":
                    authentifier(exchange);
                    return new Reponse(200, gson.toJson(categorieController.obtenir(id)));
                case "PUT": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    CategorieRequest r = lireCorps(exchange, CategorieRequest.class);
                    exiger(r.nom(), "nom");
                    return new Reponse(200, gson.toJson(categorieController.modifier(id, r)));
                }
                case "DELETE":
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    categorieController.supprimer(id);
                    return new Reponse(204, "");
                default:
                    throw new MethodNotAllowedException(methode, "/categories/{id}", "voir README");
            }
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /stands/{id} (exposant, administrateur) | PUT, DELETE /stands/{id} (administrateur) : BF-12 a BF-14
    // (creation : POST /events/{id}/stands ; plan : GET /events/{id}/stands)
    private Reponse routesStands(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 3) {
            int id = entier(segments[2]);
            switch (methode) {
                case "GET":
                    exigerRole(exchange, Role.EXPOSANT.name(), Role.ADMINISTRATEUR.name());
                    return new Reponse(200, gson.toJson(standController.stand(id)));
                case "PUT": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    StandRequest r = lireCorps(exchange, StandRequest.class);
                    return new Reponse(200, gson.toJson(standController.modifierStand(id,
                            exiger(r.categorieId(), "categorieId"), exiger(r.numero(), "numero"),
                            r.emplacement(), exiger(r.superficie(), "superficie"), exiger(r.prix(), "prix"))));
                }
                case "DELETE":
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    standController.supprimerStand(id);
                    return new Reponse(204, "");
                default:
                    break;
            }
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // PUT /ticket-types/{id} | DELETE /ticket-types/{id}   (administrateur) : BF-29, BF-30
    // (creation : POST /events/{id}/ticket-types ; liste : GET /events/{id}/ticket-types)
    private Reponse routesTypesBillet(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 3) {
            int id = entier(segments[2]);
            switch (methode) {
                case "PUT": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    TypeBilletRequest r = lireCorps(exchange, TypeBilletRequest.class);
                    return new Reponse(200, gson.toJson(billetController.modifierType(id,
                            exiger(r.nom(), "nom"), exiger(r.prix(), "prix"), exiger(r.quota(), "quota"))));
                }
                case "DELETE":
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    billetController.supprimerType(id);
                    return new Reponse(204, "");
                default:
                    break;
            }
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
    }

    // GET /sessions/{id} (public) | PUT, DELETE /sessions/{id} (administrateur) : BF-24, BF-25
    // POST /sessions/{id}/registration | DELETE /sessions/{id}/registration (visiteur) : BF-27
    private Reponse routesSessions(String methode, String[] segments, HttpExchange exchange) throws IOException {
        if (segments.length == 3) {
            int id = entier(segments[2]);
            switch (methode) {
                case "GET":
                    return new Reponse(200, gson.toJson(sessionController.obtenir(id)));
                case "PUT": {
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    SessionRequest r = lireCorps(exchange, SessionRequest.class);
                    return new Reponse(200, gson.toJson(sessionController.modifier(id, r)));
                }
                case "DELETE":
                    exigerRole(exchange, Role.ADMINISTRATEUR.name());
                    sessionController.supprimer(id);
                    return new Reponse(204, "");
                default:
                    break;
            }
        }
        if (segments.length == 4 && "registration".equals(segments[3])) {
            JwtUtil.Claims claims = exigerRole(exchange, Role.VISITEUR.name());
            int id = entier(segments[2]);
            if ("POST".equals(methode)) {
                return new Reponse(201, gson.toJson(sessionController.sInscrire(claims.userId(), id)));
            }
            if ("DELETE".equals(methode)) {
                sessionController.seDesinscrire(claims.userId(), id);
                return new Reponse(204, "");
            }
        }
        throw new ResourceNotFoundException("Route inconnue : " + String.join("/", segments));
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

    private EmailRequest exigerEmail(EmailRequest r) {
        exiger(r.email(), "email");
        return r;
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

    private void envoyerPdf(HttpExchange exchange, byte[] pdf, String nomFichier) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/pdf");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + nomFichier + "\"");
        // Le front (autre origine) doit pouvoir lire le nom du fichier
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Disposition");
        exchange.sendResponseHeaders(200, pdf.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(pdf);
        }
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