package tn.enicarthage.gestionsalon.controller;

import tn.enicarthage.gestionsalon.dto.SessionRequest;
import tn.enicarthage.gestionsalon.dto.SessionResponse;
import tn.enicarthage.gestionsalon.model.Session;
import tn.enicarthage.gestionsalon.service.SessionService;

import java.util.List;

public class SessionController {

    private final SessionService service = new SessionService();

    // Programme d'un evenement
    public List<SessionResponse> programme(int evenementId) {
        return service.getSessionsByEvenement(evenementId).stream().map(this::vue).toList();
    }

    public SessionResponse obtenir(int id) {
        return vue(service.getSessionById(id));
    }

    public SessionResponse creer(int evenementId, SessionRequest r) {
        Session s = new Session();
        s.setEvenementId(evenementId);
        remplir(s, r);
        service.createSession(s);
        return vue(s);
    }

    // L'evenement d'une session ne change pas : seul son contenu est modifiable
    public SessionResponse modifier(int id, SessionRequest r) {
        Session s = service.getSessionById(id);
        remplir(s, r);
        service.updateSession(s);
        return vue(s);
    }

    public void supprimer(int id) { service.deleteSession(id); }

    public SessionResponse sInscrire(int visiteurId, int sessionId) {
        service.inscrireVisiteur(visiteurId, sessionId);
        return obtenir(sessionId);
    }

    public void seDesinscrire(int visiteurId, int sessionId) {
        service.desinscrireVisiteur(visiteurId, sessionId);
    }

    public List<SessionResponse> mesSessions(int visiteurId) {
        return service.getSessionsByVisiteur(visiteurId).stream().map(this::vue).toList();
    }

    private void remplir(Session s, SessionRequest r) {
        s.setTitre(r.titre());
        s.setDescription(r.description());
        s.setHeureDebut(r.heureDebut());
        s.setHeureFin(r.heureFin());
        s.setCapaciteMax(r.capaciteMax());
    }

    private SessionResponse vue(Session s) {
        return new SessionResponse(s, service.getPlacesRestantes(s.getId()));
    }
}
