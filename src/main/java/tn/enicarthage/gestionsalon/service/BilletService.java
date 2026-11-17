package tn.enicarthage.gestionsalon.service;

import tn.enicarthage.gestionsalon.config.DatabaseConnection;
import tn.enicarthage.gestionsalon.dao.BilletDAO;
import tn.enicarthage.gestionsalon.dao.BilletDAO.BilletVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.ScanVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.TypeBilletVue;
import tn.enicarthage.gestionsalon.dao.BilletDAO.TypeVerrouille;
import tn.enicarthage.gestionsalon.exception.ConflictException;
import tn.enicarthage.gestionsalon.exception.DataAccessException;
import tn.enicarthage.gestionsalon.exception.ResourceNotFoundException;
import tn.enicarthage.gestionsalon.util.CodeBillet;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BilletService {

    private final BilletDAO dao = new BilletDAO();

    public List<TypeBilletVue> types(int evenementId) {
        return dao.listerTypes(evenementId);
    }

    public List<BilletVue> mesBillets(int visiteurId) {
        return dao.trouverParVisiteur(visiteurId);
    }

    public BilletVue acheter(int visiteurId, int typeBilletId) {
        int billetId;
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                TypeVerrouille type = dao.verrouillerType(c, typeBilletId);
                if (type == null) {
                    throw new ResourceNotFoundException("Type de billet introuvable.");
                }
                if (type.dateFin().isBefore(LocalDate.now())) {
                    throw new ConflictException("Cet evenement est termine.");
                }
                if (dao.existeActifPourEvenement(c, visiteurId, type.evenementId())) {
                    throw new ConflictException("Vous avez deja un billet pour cet evenement.");
                }
                if (dao.compterActifs(c, typeBilletId) >= type.quota()) {
                    throw new ConflictException("Plus de places pour ce type de billet.");
                }
                billetId = dao.inserer(c, visiteurId, typeBilletId, CodeBillet.generer(), type.prix());
                c.commit();
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL : " + e.getMessage(), e);
        }
        return dao.trouverVue(billetId);
    }

    public void annuler(int visiteurId, int billetId) {
        // 404 aussi pour le billet d'un autre : on ne revele pas son existence
        if (!dao.annuler(visiteurId, billetId)) {
            throw new ResourceNotFoundException("Billet introuvable ou non annulable.");
        }
    }

    public ScanVue scanner(int agentId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code est obligatoire.");
        }
        String codeNormalise = code.trim().toUpperCase();
        boolean ok = dao.marquerUtilise(agentId, codeNormalise);
        ScanVue info = dao.infoScan(codeNormalise);
        if (info == null) {
            throw new ResourceNotFoundException("Billet inconnu.");
        }
        if (!ok) {
            throw new ConflictException("UTILISE".equals(info.statut())
                    ? "Billet deja valide." : "Billet annule.");
        }
        return info;
    }
}