package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.dao.DemandeStandDAO;
import tn.enicarthage.gestionsalon.dao.StandDAO;
import tn.enicarthage.gestionsalon.dao.StandVueDAO;
import tn.enicarthage.gestionsalon.dao.StandVueDAO.DemandeVue;
import tn.enicarthage.gestionsalon.dao.StandVueDAO.StandVue;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.DemandeStand;
import tn.enicarthage.gestionsalon.model.Stand;
import tn.enicarthage.gestionsalon.model.StatutDemande;
import tn.enicarthage.gestionsalon.model.StatutStand;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class StandService {

    private final StandDAO standDAO = new StandDAO();
    private final DemandeStandDAO demandeDAO = new DemandeStandDAO();
    private final StandVueDAO vueDAO = new StandVueDAO();

    public List<StandVue> stands(int evenementId) { return vueDAO.listerStands(evenementId); }
    public List<DemandeVue> mesDemandes(int exposantId) { return vueDAO.demandesDeLExposant(exposantId); }
    public List<DemandeVue> toutesLesDemandes() { return vueDAO.toutesLesDemandes(); }

    public DemandeVue demander(int exposantId, int standId) {
        int demandeId;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Stand stand = standDAO.findByIdForUpdate(c, standId); // verrou
                if (stand == null) {
                    throw new ResourceNotFoundException("Stand introuvable.");
                }
                if (stand.getStatut() == StatutStand.RESERVE) {
                    throw new ConflictException("Ce stand est deja reserve.");
                }
                if (demandeDAO.existeEnAttente(c, exposantId, standId)) {
                    throw new ConflictException("Vous avez deja une demande en attente pour ce stand.");
                }
                DemandeStand d = new DemandeStand();
                d.setExposantId(exposantId);
                d.setStandId(standId);
                d.setStatut(StatutDemande.EN_ATTENTE);
                demandeDAO.create(c, d);
                if (stand.getStatut() == StatutStand.LIBRE) {
                    standDAO.updateStatut(c, standId, StatutStand.EN_ATTENTE);
                }
                c.commit();
                demandeId = d.getId();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return vueDAO.trouverDemande(demandeId);
    }

    public DemandeVue traiter(int adminId, int demandeId, String decision, String motif) {
        if (!"VALIDEE".equals(decision) && !"REFUSEE".equals(decision)) {
            throw new IllegalArgumentException("Decision invalide : VALIDEE ou REFUSEE.");
        }
        boolean valider = "VALIDEE".equals(decision);
        if (!valider && (motif == null || motif.isBlank())) {
            throw new IllegalArgumentException("Le motif du refus est obligatoire.");
        }

        DemandeStand demande = demandeDAO.findById(demandeId);
        if (demande == null) {
            throw new ResourceNotFoundException("Demande introuvable.");
        }

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Meme ordre de verrouillage partout : le stand d'abord
                Stand stand = standDAO.findByIdForUpdate(c, demande.getStandId());
                if (stand == null) {
                    throw new ResourceNotFoundException("Stand introuvable.");
                }
                demande.setAdminId(adminId);
                demande.setStatut(valider ? StatutDemande.VALIDEE : StatutDemande.REFUSEE);
                demande.setMotifRefus(valider ? null : motif.trim());

                if (valider && stand.getStatut() == StatutStand.RESERVE) {
                    throw new ConflictException("Ce stand est deja reserve.");
                }
                if (!demandeDAO.updateSiEnAttente(c, demande)) {
                    throw new ConflictException("Cette demande a deja ete traitee.");
                }
                if (valider) {
                    vueDAO.reserver(c, demande.getStandId(), demande.getExposantId());
                    demandeDAO.refuserAutresEnAttente(c, demande.getStandId(), demandeId, adminId);
                } else if (demandeDAO.countEnAttenteByStand(c, demande.getStandId()) == 0) {
                    standDAO.updateStatutSiEnAttente(c, demande.getStandId(), StatutStand.LIBRE);
                }
                c.commit();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return vueDAO.trouverDemande(demandeId);
    }
}