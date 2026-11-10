package tn.enicarthage.gestionSalon.service;

import tn.enicarthage.gestionSalon.dao.BilletDAO;
import tn.enicarthage.gestionSalon.dao.EvenementDAO;
import tn.enicarthage.gestionSalon.dao.UtilisateurDAO;
import tn.enicarthage.gestionSalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionSalon.model.Billet;
import tn.enicarthage.gestionSalon.model.Evenement;
import tn.enicarthage.gestionSalon.model.Role;
import tn.enicarthage.gestionSalon.model.StatutBillet;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BilletService {

    private final BilletDAO billetDAO;
    private final EvenementDAO evenementDAO;
    private final UtilisateurDAO utilisateurDAO;

    public BilletService() {
        this.billetDAO = new BilletDAO();
        this.evenementDAO = new EvenementDAO();
        this.utilisateurDAO = new UtilisateurDAO();
    }

    // Un visiteur reserve un billet
    public Billet reserverBillet(int visiteurId, int evenementId) {
        Role role = utilisateurDAO.findRole(visiteurId);
        if (role == null) {
            throw new ResourceNotFoundException("Visiteur introuvable : " + visiteurId);
        }
        if (role != Role.VISITEUR) {
            throw new IllegalArgumentException("Seul un visiteur peut acheter un billet.");
        }

        Evenement evenement = evenementDAO.findById(evenementId);
        if (evenement == null) {
            throw new ResourceNotFoundException("Evenement introuvable : " + evenementId);
        }
        if (evenement.getDateFin().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cet evenement est termine.");
        }

        for (Billet existant : billetDAO.findByVisiteurId(visiteurId)) {
            if (existant.getEvenementId() == evenementId
                    && existant.getStatut() != StatutBillet.ANNULE) {
                throw new IllegalArgumentException(
                        "Ce visiteur a deja un billet pour cet evenement.");
            }
        }

        Billet billet = new Billet();
        billet.setVisiteurId(visiteurId);
        billet.setEvenementId(evenementId);
        billet.setCodeQR("QR-" + UUID.randomUUID());
        billet.setStatut(StatutBillet.VALIDE);
        billetDAO.create(billet);

        return billetDAO.findById(billet.getId());
    }

    // Un agent d'accueil scanne un billet a l'entree
    public void scannerBillet(String codeQr, int agentId) {
        Role role = utilisateurDAO.findRole(agentId);
        if (role == null) {
            throw new ResourceNotFoundException("Agent introuvable : " + agentId);
        }
        if (role != Role.AGENT_ACCUEIL) {
            throw new IllegalArgumentException("Seul un agent d'accueil peut scanner un billet.");
        }

        Billet billet = billetDAO.findByCodeQr(codeQr);
        if (billet == null) {
            throw new ResourceNotFoundException("Billet introuvable : " + codeQr);
        }
        if (billet.getStatut() == StatutBillet.UTILISE) {
            throw new IllegalArgumentException("Ce billet a deja ete utilise.");
        }
        if (billet.getStatut() == StatutBillet.ANNULE) {
            throw new IllegalArgumentException("Ce billet est annule.");
        }

        billet.setStatut(StatutBillet.UTILISE);
        billet.setAgentId(agentId);
        billetDAO.update(billet);
    }

    // Annulation d'un billet
    public void annulerBillet(int billetId) {
        Billet billet = getBilletById(billetId);
        if (billet.getStatut() == StatutBillet.UTILISE) {
            throw new IllegalArgumentException("Un billet utilise ne peut pas etre annule.");
        }
        if (billet.getStatut() == StatutBillet.ANNULE) {
            throw new IllegalArgumentException("Ce billet est deja annule.");
        }

        billet.setStatut(StatutBillet.ANNULE);
        billetDAO.update(billet);
    }

    public Billet getBilletById(int id) {
        Billet billet = billetDAO.findById(id);
        if (billet == null) {
            throw new ResourceNotFoundException("Billet introuvable : " + id);
        }
        return billet;
    }

    public List<Billet> getBilletsByVisiteur(int visiteurId) {
        return billetDAO.findByVisiteurId(visiteurId);
    }
}