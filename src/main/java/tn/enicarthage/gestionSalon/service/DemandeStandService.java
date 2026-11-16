package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.dao.DemandeStandDAO;
import tn.enicarthage.gestionsalon.dao.ExposantDAO;
import tn.enicarthage.gestionsalon.dao.StandDAO;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.model.DemandeStand;
import tn.enicarthage.gestionsalon.model.Stand;
import tn.enicarthage.gestionsalon.model.StatutDemande;
import tn.enicarthage.gestionsalon.model.StatutStand;
import tn.enicarthage.gestionsalon.util.Transaction;

import java.util.List;

public class DemandeStandService {

    private final DemandeStandDAO demandeDAO = new DemandeStandDAO();
    private final StandDAO standDAO = new StandDAO();
    private final ExposantDAO exposantDAO = new ExposantDAO();

    public DemandeStand createDemande(int exposantId, int standId) {
        if (!exposantDAO.exists(exposantId)) {
            throw new ResourceNotFoundException("Exposant introuvable : " + exposantId);
        }
        Stand stand = standDAO.findById(standId);
        if (stand == null) {
            throw new ResourceNotFoundException("Stand introuvable : " + standId);
        }
        if (stand.getStatut() == StatutStand.RESERVE) {
            throw new IllegalArgumentException("Ce stand est deja reserve.");
        }
        for (DemandeStand existante : demandeDAO.findByStandId(standId)) {
            if (existante.getExposantId() == exposantId
                    && existante.getStatut() == StatutDemande.EN_ATTENTE) {
                throw new IllegalArgumentException(
                        "Cet exposant a deja une demande en cours sur ce stand.");
            }
        }

        DemandeStand demande = new DemandeStand();
        demande.setExposantId(exposantId);
        demande.setStandId(standId);
        demande.setStatut(StatutDemande.EN_ATTENTE);

        Transaction.run(connection -> {
            demandeDAO.create(connection, demande);
            if (stand.getStatut() == StatutStand.LIBRE) {
                standDAO.updateStatut(connection, standId, StatutStand.EN_ATTENTE);
            }
            return null;
        });
        return demande;
    }

    public void validerDemande(int demandeId, int adminId) {
        DemandeStand demande = getDemandeById(demandeId);
        verifierEnAttente(demande);

        Transaction.run(connection -> {
            demande.setStatut(StatutDemande.VALIDEE);
            demande.setAdminId(adminId);
            demande.setMotifRefus(null);

            if (!demandeDAO.updateSiEnAttente(connection, demande)) {
                throw new IllegalArgumentException("La demande n'est plus en attente.");
            }
            standDAO.updateStatut(connection, demande.getStandId(), StatutStand.RESERVE);
            demandeDAO.refuserAutresEnAttente(
                    connection, demande.getStandId(), demande.getId(), adminId);
            return null;
        });
    }

    public void refuserDemande(int demandeId, int adminId, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de refus est obligatoire.");
        }
        DemandeStand demande = getDemandeById(demandeId);
        verifierEnAttente(demande);

        Transaction.run(connection -> {
            demande.setStatut(StatutDemande.REFUSEE);
            demande.setAdminId(adminId);
            demande.setMotifRefus(motif);

            if (!demandeDAO.updateSiEnAttente(connection, demande)) {
                throw new IllegalArgumentException("La demande n'est plus en attente.");
            }
            if (demandeDAO.countEnAttenteByStand(connection, demande.getStandId()) == 0) {
                standDAO.updateStatutSiEnAttente(
                        connection, demande.getStandId(), StatutStand.LIBRE);
            }
            return null;
        });
    }

    public DemandeStand getDemandeById(int id) {
        DemandeStand demande = demandeDAO.findById(id);
        if (demande == null) {
            throw new ResourceNotFoundException("Demande introuvable : " + id);
        }
        return demande;
    }

    public List<DemandeStand> getAllDemandes() {
        return demandeDAO.findAll();
    }

    public List<DemandeStand> getDemandesByExposant(int exposantId) {
        return demandeDAO.findByExposantId(exposantId);
    }

    private void verifierEnAttente(DemandeStand demande) {
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new IllegalArgumentException("Seule une demande en attente peut etre traitee.");
        }
    }
}