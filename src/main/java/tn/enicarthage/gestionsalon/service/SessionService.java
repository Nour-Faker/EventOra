package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.BilletDAO;
import tn.enicarthage.gestionsalon.dao.EvenementDAO;
import tn.enicarthage.gestionsalon.dao.InscriptionSessionDAO;
import tn.enicarthage.gestionsalon.dao.SessionDAO;
import tn.enicarthage.gestionsalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.Evenement;
import tn.enicarthage.gestionsalon.model.Role;
import tn.enicarthage.gestionsalon.model.Session;

import java.time.LocalDate;
import java.util.List;

public class SessionService {

    private final SessionDAO sessionDAO;
    private final InscriptionSessionDAO inscriptionDAO;
    private final EvenementDAO evenementDAO;
    private final UtilisateurDAO utilisateurDAO;
    private final BilletDAO billetDAO = new BilletDAO();

    public SessionService() {
        this.sessionDAO = new SessionDAO();
        this.inscriptionDAO = new InscriptionSessionDAO();
        this.evenementDAO = new EvenementDAO();
        this.utilisateurDAO = new UtilisateurDAO();
    }

    // ----- Gestion des sessions (administrateur) -----

    public void createSession(Session session) {
        validate(session);
        sessionDAO.create(session);
    }

    public List<Session> getSessionsByEvenement(int evenementId) {
        return sessionDAO.findByEvenementId(evenementId);
    }

    public Session getSessionById(int id) {
        Session session = sessionDAO.findById(id);
        if (session == null) {
            throw new ResourceNotFoundException("Session introuvable : " + id);
        }
        return session;
    }

    public void updateSession(Session session) {
        getSessionById(session.getId());
        validate(session);
        if (session.getCapaciteMax() < inscriptionDAO.countBySession(session.getId())) {
            throw new ConflictException(
                    "La capacite ne peut pas etre inferieure au nombre d'inscrits.");
        }
        sessionDAO.update(session);
    }

    public void deleteSession(int id) {
        getSessionById(id);
        sessionDAO.delete(id);
    }

    // ----- Inscriptions des visiteurs -----

    public void inscrireVisiteur(int visiteurId, int sessionId) {
        Role role = utilisateurDAO.findRole(visiteurId);
        if (role == null) {
            throw new ResourceNotFoundException("Visiteur introuvable : " + visiteurId);
        }
        if (role != Role.VISITEUR) {
            throw new IllegalArgumentException(
                    "Seul un visiteur peut s'inscrire a une session.");
        }

        Session session = getSessionById(sessionId);

        // UC "S'inscrire a une session" : il faut un billet (non annule) pour l'evenement
        if (!billetDAO.possedeBilletActif(visiteurId, session.getEvenementId())) {
            throw new ConflictException(
                    "Vous devez d'abord reserver un billet pour cet evenement.");
        }
        if (inscriptionDAO.exists(visiteurId, sessionId)) {
            throw new ConflictException("Ce visiteur est deja inscrit a cette session.");
        }
        if (inscriptionDAO.countBySession(sessionId) >= session.getCapaciteMax()) {
            throw new ConflictException("Cette session est complete.");
        }

        inscriptionDAO.add(visiteurId, sessionId);
    }

    public void desinscrireVisiteur(int visiteurId, int sessionId) {
        getSessionById(sessionId);
        if (!inscriptionDAO.exists(visiteurId, sessionId)) {
            throw new IllegalArgumentException("Ce visiteur n'est pas inscrit a cette session.");
        }
        inscriptionDAO.remove(visiteurId, sessionId);
    }

    public int getPlacesRestantes(int sessionId) {
        Session session = getSessionById(sessionId);
        return session.getCapaciteMax() - inscriptionDAO.countBySession(sessionId);
    }

    public List<Session> getSessionsByVisiteur(int visiteurId) {
        return sessionDAO.findByVisiteurId(visiteurId);
    }

    // ----- Regles de validation -----

    private void validate(Session session) {
        if (session.getTitre() == null || session.getTitre().isBlank()) {
            throw new IllegalArgumentException("Le titre de la session est obligatoire.");
        }
        if (session.getCapaciteMax() <= 0) {
            throw new IllegalArgumentException("La capacite doit etre positive.");
        }
        if (session.getHeureDebut() == null || session.getHeureFin() == null
                || !session.getHeureFin().isAfter(session.getHeureDebut())) {
            throw new IllegalArgumentException(
                    "L'heure de fin doit etre apres l'heure de debut.");
        }

        Evenement evenement = evenementDAO.findById(session.getEvenementId());
        if (evenement == null) {
            throw new ResourceNotFoundException(
                    "Evenement introuvable : " + session.getEvenementId());
        }

        LocalDate jourDebut = session.getHeureDebut().toLocalDate();
        LocalDate jourFin = session.getHeureFin().toLocalDate();
        if (jourDebut.isBefore(evenement.getDateDebut())
                || jourFin.isAfter(evenement.getDateFin())) {
            throw new IllegalArgumentException(
                    "La session doit avoir lieu pendant l'evenement.");
        }
    }
}