package tn.enicarthage.gestionsalon.controller;

import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.gestionsalon.dto.DecisionRequest;
import tn.enicarthage.gestionsalon.dto.DemandeStandRequest;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.service.DemandeStandService;
import tn.enicarthage.gestionsalon.util.AuthUser;

/**
 * POST /stand-requests               EXPOSANT  (userId pris dans le token)
 * GET  /stand-requests/mine          EXPOSANT  (filtré par le token)
 * GET  /stand-requests               ADMIN
 * PUT  /stand-requests/{id}/validate ADMIN
 * PUT  /stand-requests/{id}/reject   ADMIN
 *
 * ADAPTEZ aux signatures réelles de DemandeStandService :
 *   creer(int exposantId, DemandeStandRequest req)
 *   listerParExposant(int exposantId) / listerToutes()
 *   valider(int demandeId, int adminId) / rejeter(int demandeId, int adminId, String motif)
 */
public class DemandeStandController extends BaseController {

    private final DemandeStandService service;

    public DemandeStandController(DemandeStandService service) { this.service = service; }

    @Override
    protected void route(HttpExchange ex) throws Exception {
        String[] s = segments(ex);
        String method = ex.getRequestMethod();

        if (s.length == 1) {                                   // /stand-requests
            switch (method) {
                case "POST" -> {
                    AuthUser u = requireRole(ex, Role.EXPOSANT);
                    DemandeStandRequest body = readBody(ex, DemandeStandRequest.class);
                    sendJson(ex, 201, service.creer(u.id(), body));
                }
                case "GET" -> {
                    requireRole(ex, Role.ADMIN);
                    sendJson(ex, 200, service.listerToutes());
                }
                default -> methodNotAllowed(ex);
            }
            return;
        }

        if (s.length == 2 && "mine".equals(s[1])) {            // /stand-requests/mine
            if (!"GET".equals(method)) { methodNotAllowed(ex); return; }
            AuthUser u = requireRole(ex, Role.EXPOSANT);
            sendJson(ex, 200, service.listerParExposant(u.id()));
            return;
        }

        if (s.length == 3) {                                   // /stand-requests/{id}/validate|reject
            if (!"PUT".equals(method)) { methodNotAllowed(ex); return; }
            AuthUser admin = requireRole(ex, Role.ADMIN);
            int id = parseId(s[1]);
            switch (s[2]) {
                case "validate" -> sendJson(ex, 200, service.valider(id, admin.id()));
                case "reject" -> {
                    DecisionRequest body = readBody(ex, DecisionRequest.class);
                    if (body.motif() == null || body.motif().isBlank())   // ADAPTEZ : champ de DecisionRequest
                        throw new IllegalArgumentException("Motif obligatoire");
                    sendJson(ex, 200, service.rejeter(id, admin.id(), body.motif()));
                }
                default -> throw new ResourceNotFoundException("Route inconnue");
            }
            return;
        }

        throw new ResourceNotFoundException("Route inconnue");
    }
}