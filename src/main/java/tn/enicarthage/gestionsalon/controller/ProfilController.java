package tn.enicarthage.gestionsalon.controller;

import com.sun.net.httpserver.HttpExchange;
import tn.enicarthage.gestionsalon.dto.ChangePasswordRequest;
import tn.enicarthage.gestionsalon.dto.UpdateProfilRequest;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.service.ProfilService;
import tn.enicarthage.gestionsalon.util.AuthUser;

import java.util.Map;

/**
 * GET /me             tout utilisateur connecté
 * PUT /me             idem
 * PUT /me/password    idem
 * L'id vient TOUJOURS du token : impossible de lire/modifier le profil d'un autre.
 */
public class ProfilController extends BaseController {

    private final ProfilService service;

    public ProfilController(ProfilService service) { this.service = service; }

    @Override
    protected void route(HttpExchange ex) throws Exception {
        AuthUser u = requireAuth(ex);
        String[] s = segments(ex);
        String method = ex.getRequestMethod();

        if (s.length == 1) {                                      // /me
            switch (method) {
                case "GET" -> sendJson(ex, 200, service.getProfil(u.id()));
                case "PUT" -> sendJson(ex, 200,
                        service.updateProfil(u.id(), readBody(ex, UpdateProfilRequest.class)));
                default -> methodNotAllowed(ex);
            }
            return;
        }

        if (s.length == 2 && "password".equals(s[1])) {           // /me/password
            if (!"PUT".equals(method)) { methodNotAllowed(ex); return; }
            service.changePassword(u.id(), readBody(ex, ChangePasswordRequest.class));
            sendJson(ex, 200, Map.of("message", "Mot de passe modifié"));
            return;
        }

        throw new ResourceNotFoundException("Route inconnue");
    }
}