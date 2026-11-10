package tn.enicarthage.gestionSalon.service;

import tn.enicarthage.gestionSalon.dao.DemandeStandDAO;
import tn.enicarthage.gestionSalon.dao.ExposantDAO;
import tn.enicarthage.gestionSalon.dao.StandDAO;
import tn.enicarthage.gestionSalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionSalon.model.DemandeStand;
import tn.enicarthage.gestionSalon.model.Stand;
import tn.enicarthage.gestionSalon.model.StatutDemande;
import tn.enicarthage.gestionSalon.model.StatutStand;

import java.util.List;

public class DemandeStandService {

    private final DemandeStandDAO demandeDAO;
    private final StandDAO standDAO;
    private final ExposantDAO exposantDAO;

    public DemandeStandService() {
        this.demandeDAO = new DemandeStandDAO();
        this.standDAO = new StandDAO();
        this.exposantDAO = new ExposantDAO();
    }

    // Un exposant demande un stand
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
        demandeDAO.create(demande);

        if (stand.getStatut() == StatutStand.LIBRE) {
            stand.setStatut(StatutStand.EN_ATTENTE);
            standDAO.update(stand);
        }

        return demande;
    }

    // L'administrateur valide une demande
    public void validerDemande(int demandeId, int adminId) {
        DemandeStand demande = getDemandeById(demandeId);
        verifierEnAttente(demande);

        Stand stand = standDAO.findById(demande.getStandId());

        demande.setStatut(StatutDemande.VALIDEE);
        demande.setAdminId(adminId);
        demandeDAO.update(demande);

        stand.setStatut(StatutStand.RESERVE);
        standDAO.update(stand);

        // Les autres demandes en attente sur ce stand sont refusees
        for (DemandeStand autre : demandeDAO.findByStandId(stand.getId())) {
            if (autre.getId() != demande.getId()
                    && autre.getStatut() == StatutDemande.EN_ATTENTE) {
                autre.setStatut(StatutDemande.REFUSEE);
                autre.setAdminId(adminId);
                autre.setMotifRefus("Stand attribue a un autre exposant");
                demandeDAO.update(autre);
            }
        }
    }

    // L'administrateur refuse une demande
    public void refuserDemande(int demandeId, int adminId, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de refus est obligatoire.");
        }

        DemandeStand demande = getDemandeById(demandeId);
        verifierEnAttente(demande);

        demande.setStatut(StatutDemande.REFUSEE);
        demande.setAdminId(adminId);
        demande.setMotifRefus(motif);
        demandeDAO.update(demande);

        // Si plus aucune demande n'est en attente, le stand redevient libre
        Stand stand = standDAO.findById(demande.getStandId());
        boolean encoreEnAttente = false;
        for (DemandeStand autre : demandeDAO.findByStandId(stand.getId())) {
            if (autre.getStatut() == StatutDemande.EN_ATTENTE) {
                encoreEnAttente = true;
            }
        }
        if (!encoreEnAttente && stand.getStatut() == StatutStand.EN_ATTENTE) {
            stand.setStatut(StatutStand.LIBRE);
            standDAO.update(stand);
        }
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
            throw new IllegalArgumentException(
                    "Seule une demande en attente peut etre traitee.");
        }
    }
}